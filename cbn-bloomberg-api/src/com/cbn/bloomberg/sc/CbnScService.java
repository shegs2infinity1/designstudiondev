package com.cbn.bloomberg.sc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cbn.bloomberg.sc.CbnScAdapter.FileItemRef;
import com.cbn.bloomberg.sc.CbnScAdapter.MqItemRef;
import com.cbn.bloomberg.util.CbnTfProperties;
import com.cbn.bloomberg.util.CbnTfLogTracer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temenos.api.TBoolean;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.records.securitymaster.DescriptClass;
import com.temenos.t24.api.records.securitymaster.InterestRateClass;
import com.temenos.t24.api.records.securitymaster.SecurityMasterRecord;
import com.temenos.t24.api.records.sectrade.SecTradeRecord;
import com.temenos.t24.api.records.sectrade.CustomerNoClass;
import com.temenos.t24.api.records.sectrade.CustNoNomClass;
import com.temenos.t24.api.records.sectrade.BrokerNoClass;
import com.temenos.t24.api.records.sectrade.TradeCurrClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;


/**
 * =============================================================================
 * CSD API Title: CbnScService.java
 * Author: CSD Development Team
 * Created: 2026-01-07
 * Last Modified: 2026-02-03
 * =============================================================================
 *
 * PURPOSE: Bloomberg SC Service Hook supporting dual ingestion modes: FILE or WMQ (IBM MQ).
 * The adapter mode is controlled by bloomberg.properties.
 *
 * TARGETS:
 * - SECURITY_MASTER → T24 SECURITY.MASTER table (static reference data)
 * - SEC_TRADE → T24 SEC.TRADE table (trading transactions)
 *
 * TWO-PHASE PROCESSING PATTERN:
 * Phase 1 (PROCESS.SC): Submit OFS request to T24 with responseId
 * Phase 2 (CHECK.RESPONSE): Retrieve OFS response from OFS.REQUEST.DETAIL and publish to MQ
 *
 * MODIFICATION HISTORY:
 * - 2026-01-07 | Initial creation for SECURITY_MASTER
 * - 2026-02-03 | Added SEC_TRADE support (routing, buildStRecord, message type detection)
 * =============================================================================
 */
public class CbnScService extends ServiceLifecycle {

    // ==== CONSTANTS ====
    private static final Logger yLOGGER = CbnTfLogTracer.forClass(CbnScService.class);
    private static final CbnTfProperties CONFIG = CbnTfProperties.getInstance();

    // Control list constants
    private static final String CONTROL_PROCESS_SC = "PROCESS.SC";
    private static final String CONTROL_CHECK_RESPONSE = "CHECK.RESPONSE";

    // Message type constants
    private static final String MSG_TYPE_SC = "SC"; // SECURITY_MASTER
    private static final String MSG_TYPE_ST = "ST"; // SEC_TRADE

    // Message constants
    private static final String MSG_SUCCESS = "success";
    private static final String MSG_FAILURE = "failure";
    private static final String MSG_UNKNOWN = "UNKNOWN";

    // Log prefix
    private static final String LOG_PREFIX = "[CbnScService] ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==== CONFIGURATION ====
    private final String mAdapterFlag = CONFIG.getDefAdapter();
    private final String mOfsSource = CONFIG.getOfsSource();
    private final String mOfsVersionSc = CONFIG.getOfsVersionSc(); // For SECURITY_MASTER
    private final String mOfsVersionSt = CONFIG.getOfsVersionSt(); // For SEC_TRADE
    private final String mOfsFunction = CONFIG.getOfsFunction();

    private final Path mInboundDir = Paths.get(CONFIG.getNfsInboundDir());
    private final Path mProcessDir = Paths.get(CONFIG.getNfsDoneDir());
    private final Path mExceptsDir = Paths.get(CONFIG.getNfsErrorDir());
    private final String mFilePattern = CONFIG.getNfsFilePattern();

    // ==== INSTANCE VARIABLES ====
    private String mCompanyId = "BNK";
    private Session mSession = null;
    private DataAccess mDataAccess = null;
    private final CbnScPayload mPayloadHandler = new CbnScPayload(OBJECT_MAPPER);
    private final CbnScProducer mProducer = new CbnScProducer();

    // ==== TRANSACTION METADATA CACHE ====
    private static final Map<String, TransactionMetadata> TRANSACTION_CACHE = new HashMap<>();

    /**
     * Metadata holder for tracking transaction state between phases.
     * Now includes message type (SC or ST) for routing.
     */
    private static class TransactionMetadata {

        String originalId;
        JsonNode originalItem;
        String adapterMode;
        String messageType; // "SC" or "ST"

        TransactionMetadata(String pOriginalId, JsonNode pOriginalItem, String pAdapterMode,
                String pMessageType) {
            this.originalId = pOriginalId;
            this.originalItem = pOriginalItem;
            this.adapterMode = pAdapterMode;
            this.messageType = pMessageType;
        }
    }

    /**
     * Initializes the service with session and data access objects.
     */
    @Override
    public void initialise(ServiceData serviceData) {
        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== initialise() START ===");
            mSession = new Session(this);
            mDataAccess = new DataAccess(this);
            mCompanyId = mSession.getCompanyId();

            yLOGGER.log(Level.INFO, LOG_PREFIX + "initialise: Service initialized for company: {0}",
                    mCompanyId);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== initialise() COMPLETE ===");
        } catch (T24CoreException e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "initialise: Error initializing service", e);
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE,
                    LOG_PREFIX + "initialise: Unexpected error during initialization", e);
        }
    }

    /**
     * Retrieves the list of transaction IDs to be processed by the service.
     */
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> pRecordIds = null;
        String pControlItem = null;

        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getIds() START ===");

            if (mSession == null || mDataAccess == null) {
                yLOGGER.log(Level.WARNING,
                        LOG_PREFIX + "getIds: Service not initialized, initializing now");
                initialise(serviceData);
            }

            if (controlList == null || controlList.isEmpty()) {
                if (controlList == null) {
                    controlList = new ArrayList<>();
                }
                controlList.add(0, CONTROL_PROCESS_SC);
                controlList.add(1, CONTROL_CHECK_RESPONSE);
            }

            pControlItem = controlList.get(0);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, adapterFlag={1}",
                    new Object[] { pControlItem, mAdapterFlag });

            switch (pControlItem) {
                case CONTROL_PROCESS_SC:
                    pRecordIds = getNewTransactionIds();
                    break;

                case CONTROL_CHECK_RESPONSE:
                    pRecordIds = getPendingResponseIds();
                    break;

                default:
                    yLOGGER.log(Level.WARNING, LOG_PREFIX
                            + "getIds: Unrecognized controlList option: {0}. Returning empty list.",
                            pControlItem);
                    pRecordIds = Collections.emptyList();
                    break;
            }

            yLOGGER.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, total IDs={1}",
                    new Object[] { pControlItem, pRecordIds.size() });
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getIds() COMPLETE ===");

        } catch (T24CoreException e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "getIds: Error retrieving record IDs", e);
            pRecordIds = Collections.emptyList();
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "getIds: Unexpected error in getIds", e);
            pRecordIds = Collections.emptyList();
        } finally {
            pControlItem = null;
        }
        return pRecordIds;
    }

    /**
     * Gets new transaction IDs from FILE or WMQ sources.
     * Now handles both SECURITY_MASTER (SC) and SEC_TRADE (ST) messages.
     */
    private List<String> getNewTransactionIds() {
        List<String> pIds = new ArrayList<>();

        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getNewTransactionIds() START ===");

            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                yLOGGER.log(Level.INFO,
                        LOG_PREFIX + "getNewTransactionIds: FILE mode: scanning {0}", mInboundDir);

                if (!Files.isDirectory(mInboundDir)) {
                    yLOGGER.log(Level.WARNING,
                            LOG_PREFIX + "getNewTransactionIds: Inbound directory not found: {0}",
                            mInboundDir);
                    return pIds;
                }

                pIds.addAll(CbnScAdapter.scanDirectoryIds(mInboundDir, mFilePattern, mProcessDir,
                        OBJECT_MAPPER));

            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                yLOGGER.log(Level.INFO,
                        LOG_PREFIX + "getNewTransactionIds: WMQ mode: consuming messages from MQ");
                pIds.addAll(CbnScAdapter.extractIdsFromWmq(OBJECT_MAPPER));

            } else {
                yLOGGER.log(Level.SEVERE,
                        LOG_PREFIX + "getNewTransactionIds: Unknown adapter flag: {0}",
                        mAdapterFlag);
            }

            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "=== getNewTransactionIds() COMPLETE - Found {0} IDs ===",
                    pIds.size());

        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE,
                    LOG_PREFIX + "getNewTransactionIds: Error retrieving new transaction IDs", e);
            pIds = Collections.emptyList();
        }

        return pIds;
    }

    /**
     * Gets pending OFS response IDs from the transaction cache.
     */
    private List<String> getPendingResponseIds() {
        List<String> pResponseIds = new ArrayList<>();

        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getPendingResponseIds() START ===");

            synchronized (TRANSACTION_CACHE) {
                pResponseIds.addAll(TRANSACTION_CACHE.keySet());
            }

            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "getPendingResponseIds: Found {0} pending responses",
                    pResponseIds.size());

        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE,
                    LOG_PREFIX + "getPendingResponseIds: Error retrieving pending response IDs", e);
            pResponseIds = Collections.emptyList();
        }

        return pResponseIds;
    }

    /**
     * Updates a single record based on the current processing phase.
     */
    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        yLOGGER.log(Level.INFO, LOG_PREFIX + "=== updateRecord() START ===");
        yLOGGER.log(Level.INFO,
                LOG_PREFIX + "updateRecord: Processing updateRecord for controlItem: {0}, ID: {1}",
                new Object[] { controlItem, id });

        try {
            if (mSession == null || mDataAccess == null) {
                yLOGGER.log(Level.WARNING,
                        LOG_PREFIX + "updateRecord: Service not initialized, initializing now");
                initialise(serviceData);
            }

            if (controlItem == null || controlItem.isEmpty()) {
                yLOGGER.log(Level.WARNING, LOG_PREFIX
                        + "updateRecord: Control item is null or empty, defaulting to PROCESS.SC");
                controlItem = CONTROL_PROCESS_SC;
            }

            switch (controlItem) {
                case CONTROL_PROCESS_SC:
                    processOfsRequest(id, transactionData, records);
                    break;

                case CONTROL_CHECK_RESPONSE:
                    checkOfsResponse(id);
                    break;

                default:
                    yLOGGER.log(Level.WARNING,
                            LOG_PREFIX + "updateRecord: Unrecognized control item: {0}",
                            controlItem);
                    break;
            }

            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== updateRecord() COMPLETE ===");

        } catch (T24CoreException e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "updateRecord: Error updating record", e);
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "updateRecord: Unexpected error in updateRecord",
                    e);
        }
    }

    /**
     * Phase 1: Process OFS request - retrieve transaction, detect type (SC/ST),
     * map to appropriate record, submit to T24.
     */
    private void processOfsRequest(String pRecordId,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        JsonNode pOriginalItem = null;
        String pResponseId = null;
        String pMessageType = MSG_TYPE_SC; // Default to SECURITY_MASTER

        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() START ===");
            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "processOfsRequest: Processing OFS request for record ID: {0}",
                    pRecordId);

            // Step 1: Detect message type from ID prefix (SC or ST)
            pMessageType = detectMessageTypeFromId(pRecordId);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Detected message type: {0}",
                    pMessageType);

            // Step 2: Retrieve the item based on adapter mode and message type
            JsonNode pItem = retrieveTransactionItem(pRecordId, pMessageType);
            if (pItem == null) {
                pMessage = "Item not found or invalid "
                        + (MSG_TYPE_ST.equals(pMessageType) ? "SEC_TRADE" : "SecurityMaster");
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", null);
                return;
            }

            pOriginalItem = pItem;
            yLOGGER.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Retrieved item for id={0}",
                    pRecordId);

            // Step 3: Map JSON to field map based on message type
            Map<String, String> pData;
            if (MSG_TYPE_ST.equals(pMessageType)) {
                pData = CbnScMapper.mapSecTradeToSt(pItem);
            } else {
                pData = CbnScMapper.mapSecurityMasterToSc(pItem);
            }

            if (pData == null || pData.isEmpty()) {
                pMessage = "Mapping returned no data";
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
                return;
            }

            yLOGGER.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Mapped data for id={0}",
                    pRecordId);

            // Step 4: Build responseId
            pResponseId = buildResponseId(pRecordId);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Built responseId: {0}",
                    pResponseId);

            // Step 5: Build and populate record based on message type
            boolean pSuccess;
            if (MSG_TYPE_ST.equals(pMessageType)) {
                pSuccess = buildStRecord(pResponseId, pData, pTransactionData, pRecords);
            } else {
                pSuccess = buildScRecord(pResponseId, pData, pTransactionData, pRecords);
            }

            if (!pSuccess) {
                pMessage = "Validation failed while building "
                        + (MSG_TYPE_ST.equals(pMessageType) ? "SecTradeRecord"
                                : "SecurityMasterRecord");
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
                return;
            }

            yLOGGER.log(Level.INFO, LOG_PREFIX
                    + "processOfsRequest: Successfully prepared OFS transaction for {0} update",
                    pMessageType);

            // Step 6: Store transaction metadata in cache for Phase 2 (include message type)
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.put(pResponseId, new TransactionMetadata(pRecordId, pOriginalItem,
                        mAdapterFlag, pMessageType));
            }

            yLOGGER.log(Level.INFO,
                    LOG_PREFIX
                            + "processOfsRequest: Transaction metadata cached for responseId={0}",
                    pResponseId);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() COMPLETE ===");

        } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
            yLOGGER.log(Level.SEVERE, jpe,
                    () -> LOG_PREFIX + "processOfsRequest: JSON parse error for id=" + pRecordId);
            pMessage = "JSON parse error: " + jpe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);

        } catch (java.io.IOException ioe) {
            yLOGGER.log(Level.SEVERE, ioe,
                    () -> LOG_PREFIX + "processOfsRequest: I/O error for id=" + pRecordId);
            pMessage = "I/O error: " + ioe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);

        } catch (RuntimeException re) {
            yLOGGER.log(Level.SEVERE, re,
                    () -> LOG_PREFIX + "processOfsRequest: Runtime error for id=" + pRecordId);
            pMessage = "Runtime error: " + re.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
        } finally {
            pOriginalItem = null;
            pResponseId = null;
        }
    }

    /**
     * Detects message type (SC or ST) from the transaction ID prefix.
     */
    private String detectMessageTypeFromId(String pId) {
        if (pId == null) {
            return MSG_TYPE_SC;
        }
        // ID format: "FILE|path|SC|index" or "WMQ|msgId|ST|index"
        String[] parts = pId.split("\\|");
        if (parts.length >= 3) {
            String prefix = parts[2];
            if (MSG_TYPE_ST.equals(prefix)) {
                return MSG_TYPE_ST;
            }
        }
        return MSG_TYPE_SC;
    }

    /**
     * Phase 2: Check OFS response - retrieve response from OFS.REQUEST.DETAIL and publish to MQ.
     */
    private void checkOfsResponse(String pResponseId) {
        OfsRequestDetailRecord pOfsRequestDetailRecord = null;
        TBoolean pExists = null;
        String pMsgOut = null;
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        String pTransactRef = "";

        yLOGGER.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() START ===");

        TransactionMetadata pMetadata;
        synchronized (TRANSACTION_CACHE) {
            pMetadata = TRANSACTION_CACHE.get(pResponseId);
        }

        if (pMetadata == null) {
            yLOGGER.log(Level.WARNING,
                    LOG_PREFIX + "checkOfsResponse: No metadata found for responseId={0}",
                    pResponseId);
            return;
        }

        try {
            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "checkOfsResponse: Checking OFS response for record ID: {0}",
                    pResponseId);

            pExists = new TBoolean(null);
            pOfsRequestDetailRecord = mDataAccess.getRequestResponse(pResponseId, pExists);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: OFS record exists: {0}",
                    pExists);

            if (pOfsRequestDetailRecord != null && pOfsRequestDetailRecord.getMsgOut() != null) {
                pMsgOut = pOfsRequestDetailRecord.getMsgOut().getValue();

                if (pMsgOut != null && !pMsgOut.isEmpty()) {
                    yLOGGER.log(Level.INFO, LOG_PREFIX
                            + "checkOfsResponse: OFS response message retrieved successfully");

                    if (pMsgOut.contains("/-1/")) {
                        pStatus = MSG_FAILURE;
                        pMessage = "T24 OFS Error: " + pMsgOut;
                        pTransactRef = "ERROR";
                        yLOGGER.log(Level.WARNING,
                                LOG_PREFIX + "checkOfsResponse: OFS Error detected: {0}", pMsgOut);

                    } else {
                        pTransactRef = pOfsRequestDetailRecord.getTransReference().toString();
                        pStatus = MSG_SUCCESS;
                        // Use message type to customize success message
                        String msgTypeDesc = MSG_TYPE_ST.equals(pMetadata.messageType) ? "SecTrade"
                                : "SecurityMaster";
                        pMessage = msgTypeDesc + " transaction processed successfully";
                        yLOGGER.log(Level.INFO,
                                LOG_PREFIX + "checkOfsResponse: Transaction successful: ref={0}",
                                pTransactRef);
                    }

                    publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                            pMetadata.originalItem);

                    if (MSG_SUCCESS.equalsIgnoreCase(pStatus)
                            && "WMQ".equalsIgnoreCase(pMetadata.adapterMode)) {
                        CbnScAdapter.acknowledgeMqMessage(pMetadata.originalId);
                        yLOGGER.log(Level.INFO,
                                LOG_PREFIX + "checkOfsResponse: MQ message acknowledged for id={0}",
                                pMetadata.originalId);
                    }

                    synchronized (TRANSACTION_CACHE) {
                        TRANSACTION_CACHE.remove(pResponseId);
                    }

                } else {
                    yLOGGER.log(Level.WARNING,
                            LOG_PREFIX + "checkOfsResponse: OFS message output is null or empty");
                }

            } else {
                yLOGGER.log(Level.WARNING, LOG_PREFIX
                        + "checkOfsResponse: OFS request detail record or message output is null");
            }

            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() COMPLETE ===");

        } catch (T24CoreException e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "checkOfsResponse: Error checking OFS response",
                    e);
            pMessage = "Error checking OFS response: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                    pMetadata.originalItem);

            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.remove(pResponseId);
            }

        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE,
                    LOG_PREFIX + "checkOfsResponse: Unexpected error in checkOfsResponse", e);
            pMessage = "Unexpected error: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                    pMetadata.originalItem);

            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.remove(pResponseId);
            }

        } finally {
            pOfsRequestDetailRecord = null;
            pExists = null;
            pMsgOut = null;
        }
    }

    /**
     * Retrieves transaction item from FILE or WMQ based on adapter mode and message type.
     */
    private JsonNode retrieveTransactionItem(String pId, String pMessageType)
            throws java.io.IOException {
        yLOGGER.log(Level.INFO,
                LOG_PREFIX + "=== retrieveTransactionItem() START for ID: {0}, type: {1} ===",
                new Object[] { pId, pMessageType });

        JsonNode result = null;
        if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            result = processFileRecord(pId, pMessageType);
        } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            result = processWmqRecord(pId, pMessageType);
        } else {
            yLOGGER.log(Level.SEVERE,
                    LOG_PREFIX + "retrieveTransactionItem: Unknown adapter flag: {0}",
                    mAdapterFlag);
        }

        yLOGGER.log(Level.INFO, LOG_PREFIX + "=== retrieveTransactionItem() COMPLETE ===");
        return result;
    }

    /**
     * Processes a FILE mode record based on message type.
     */
    private JsonNode processFileRecord(String pId, String pMessageType) throws java.io.IOException {
        FileItemRef pRef = CbnScAdapter.parseFileItemRef(pId);
        if (pRef == null) {
            yLOGGER.log(Level.WARNING,
                    LOG_PREFIX + "processFileRecord: FILE mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CbnScAdapter.readRoot(pRef.file(), OBJECT_MAPPER);

        if (MSG_TYPE_ST.equals(pMessageType)) {
            if (!CbnScMapper.hasSecTrade(pRoot)) {
                yLOGGER.log(Level.WARNING,
                        LOG_PREFIX + "processFileRecord: FILE mode: no SEC_TRADE in {0}",
                        pRef.file().getFileName());
                return null;
            }
            return CbnScMapper.getSecTradeAt(pRoot, pRef.index());
        } else {
            if (!CbnScMapper.hasSecurityMaster(pRoot)) {
                yLOGGER.log(Level.WARNING,
                        LOG_PREFIX + "processFileRecord: FILE mode: no SecurityMaster in {0}",
                        pRef.file().getFileName());
                return null;
            }
            return CbnScMapper.getSecurityMasterAt(pRoot, pRef.index());
        }
    }

    /**
     * Processes a WMQ mode record based on message type.
     */
    private JsonNode processWmqRecord(String pId, String pMessageType) throws java.io.IOException {
        MqItemRef pRef = CbnScAdapter.parseMqItemRef(pId);
        if (pRef == null) {
            yLOGGER.log(Level.WARNING,
                    LOG_PREFIX + "processWmqRecord: WMQ mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CbnScAdapter.readMqMessage(pRef.messageId(), OBJECT_MAPPER);

        if (MSG_TYPE_ST.equals(pMessageType)) {
            if (pRoot == null || !CbnScMapper.hasSecTrade(pRoot)) {
                yLOGGER.log(Level.WARNING,
                        LOG_PREFIX + "processWmqRecord: WMQ mode: no SEC_TRADE in message {0}",
                        pRef.messageId());
                return null;
            }
            return CbnScMapper.getSecTradeAt(pRoot, pRef.index());
        } else {
            if (pRoot == null || !CbnScMapper.hasSecurityMaster(pRoot)) {
                yLOGGER.log(Level.WARNING,
                        LOG_PREFIX + "processWmqRecord: WMQ mode: no SecurityMaster in message {0}",
                        pRef.messageId());
                return null;
            }
            return CbnScMapper.getSecurityMasterAt(pRoot, pRef.index());
        }
    }

    /**
     * Builds the responseId for T24 based on the adapter mode.
     */
    private String buildResponseId(String pId) {
        if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            MqItemRef pRef = CbnScAdapter.parseMqItemRef(pId);
            if (pRef != null && pRef.messageId() != null) {
                String pJmsMessageId = pRef.messageId();
                yLOGGER.log(Level.INFO,
                        LOG_PREFIX + "buildResponseId: Using JMS Message ID as responseId: {0}",
                        pJmsMessageId);
                return pJmsMessageId;
            }
        } else if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            return extractPrefix(pId);
        }
        return extractPrefix(pId);
    }

    // ========================================================================
    // T24 RECORD BUILDING - SECURITY_MASTER (Existing)
    // ========================================================================

    /**
     * Builds a SecurityMasterRecord from the mapped data and populates the transaction lists.
     */
    private boolean buildScRecord(String pResponseId, Map<String, String> pData,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {

        SecurityMasterRecord pSecurityMasterRecord = null;
        SynchronousTransactionData pTxnData = null;

        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== buildScRecord() START ===");
            yLOGGER.log(Level.INFO,
                    LOG_PREFIX
                            + "buildScRecord: Building SecurityMaster record for responseId: {0}",
                    pResponseId);

            // Extract fields
            String sCompanyName = pData.getOrDefault("CNME", "");
            String sDescription = pData.getOrDefault("SDES", "");
            String sShortName = pData.getOrDefault("SNME", "");
            String sMnemonic = pData.getOrDefault("MMNE", "");
            String sCompanyDomicile = pData.getOrDefault("CDOM", "");
            String sSecurityDomicile = pData.getOrDefault("SDOM", "");
            String sSecurityCurrency = pData.getOrDefault("SCCY", "");
            String sBondOrShare = pData.getOrDefault("BOSH", "");
            String sSubAssetType = pData.getOrDefault("SAST", "");
            String sPriceCurrency = pData.getOrDefault("PCCY", "");
            String sPriceType = pData.getOrDefault("PTYP", "");
            String sLastPrice = pData.getOrDefault("LPRC", "");
            String sPriceUpdateCode = pData.getOrDefault("PCDE", "");
            String sIndustryCode = pData.getOrDefault("ICDE", "");
            String sStockExchange = pData.getOrDefault("SEXC", "");
            String sCouponTaxCode = pData.getOrDefault("CTAX", "");
            String sInterestBasis = pData.getOrDefault("BINT", "");
            String sIntRate = pData.getOrDefault("IRTE", "");
            String sIssueDate = pData.getOrDefault("IDTE", "");
            String sMaturityDate = pData.getOrDefault("MDTE", "");
            String sNoOfPayments = pData.getOrDefault("NPAY", "");
            String sAccrualStartDate = pData.getOrDefault("ADTE", "");
            String sIntPaymentDate = pData.getOrDefault("PDTE", "");
            String sFirstCpnDate = pData.getOrDefault("CDTE", "");
            String sIsin = pData.getOrDefault("ISIN", "");
            String sSetupDate = pData.getOrDefault("SDTE", "");

            // Validate required fields
            if (sMnemonic.isEmpty() || sShortName.isEmpty() || sSecurityCurrency.isEmpty()) {
                yLOGGER.log(Level.SEVERE, LOG_PREFIX + "buildScRecord: Missing required fields");
                return false;
            }

            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "buildScRecord: Extracted fields - CNME={0}, SDES={1}, MMNE={2}",
                    new Object[] { sCompanyName, sDescription, sMnemonic });

            DescriptClass collDescript = new DescriptClass();
            collDescript.set(sDescription, 0);

            InterestRateClass collIntRate = new InterestRateClass();
            collIntRate.setInterestRate(sIntRate);

            pSecurityMasterRecord = new SecurityMasterRecord();
            pSecurityMasterRecord.setCompanyName(sCompanyName, 0);
            pSecurityMasterRecord.setDescript(collDescript, 0);
            pSecurityMasterRecord.setShortName(sShortName, 0);
            pSecurityMasterRecord.setMnemonic(sMnemonic);
            pSecurityMasterRecord.setCompanyDomicile(sCompanyDomicile);
            pSecurityMasterRecord.setSecurityDomicile(sSecurityDomicile);
            pSecurityMasterRecord.setSecurityCurrency(sSecurityCurrency);
            pSecurityMasterRecord.setBondOrShare(sBondOrShare);
            pSecurityMasterRecord.setSubAssetType(sSubAssetType);
            pSecurityMasterRecord.setPriceCurrency(sPriceCurrency);
            pSecurityMasterRecord.setPriceType(sPriceType);
            pSecurityMasterRecord.setLastPrice(sLastPrice);
            pSecurityMasterRecord.setPriceUpdateCode(sPriceUpdateCode);
            pSecurityMasterRecord.setIndustryCode(sIndustryCode);
            pSecurityMasterRecord.setStockExchange(sStockExchange);
            pSecurityMasterRecord.setCouponTaxCode(sCouponTaxCode);
            pSecurityMasterRecord.setInterestDayBasis(sInterestBasis);
            pSecurityMasterRecord.setInterestRate(collIntRate, 0);
            pSecurityMasterRecord.setIssueDate(sIssueDate);
            pSecurityMasterRecord.setMaturityDate(sMaturityDate);
            pSecurityMasterRecord.setNoOfPayments(sNoOfPayments);
            pSecurityMasterRecord.setAccrualStartDate(sAccrualStartDate);
            pSecurityMasterRecord.setIntPaymentDate(sIntPaymentDate);
            pSecurityMasterRecord.setFirstCouponDate(sFirstCpnDate);
            pSecurityMasterRecord.setISIN(sIsin);
            pSecurityMasterRecord.setSetUpDate(sSetupDate);

            yLOGGER.log(Level.INFO, LOG_PREFIX + "buildScRecord: Adding record: {0}",
                    pSecurityMasterRecord);

            pTxnData = new SynchronousTransactionData();
            pTxnData.setResponseId(pResponseId);
            pTxnData.setVersionId(mOfsVersionSc);
            pTxnData.setFunction(mOfsFunction);
            pTxnData.setNumberOfAuthoriser("0");
            pTxnData.setSourceId(mOfsSource);
            pTxnData.setCompanyId(mCompanyId);

            pTransactionData.add(pTxnData);
            pRecords.add(pSecurityMasterRecord.toStructure());
            yLOGGER.log(Level.INFO, LOG_PREFIX
                    + "buildScRecord: SECURITY_MASTER record populated successfully with responseId={0}",
                    pResponseId);
            return true;

        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE,
                    LOG_PREFIX + "buildScRecord: Error building SecurityMasterRecord", e);
            return false;
        } finally {
            pSecurityMasterRecord = null;
            pTxnData = null;
        }
    }

    // ========================================================================
    // T24 RECORD BUILDING - SEC_TRADE (New - Added 2026-02-03)
    // ========================================================================

    /**
     * Builds a SecTradeRecord from the mapped data and populates the transaction lists.
     * 
     * T24 SEC.TRADE uses nested class structure:
     * - SecTradeRecord
     * └─ CustomerNoClass (multi-value)
     * └─ CustNoNomClass (nominal/price)
     * └─ BrokerNoClass (multi-value)
     * └─ BrNoNomClass (broker nominal/price)
     * └─ TradeCurrClass (trade currency details)
     */
    private boolean buildStRecord(String pResponseId, Map<String, String> pData,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {

        SecTradeRecord pSecTradeRecord = null;
        SynchronousTransactionData pTxnData = null;

        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== buildStRecord() START ===");
            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "buildStRecord: Building SEC.TRADE record for responseId: {0}",
                    pResponseId);

            // Extract fields from mapped data
            String sSecurityNo = pData.getOrDefault("SENO", "");
            String sDepository = pData.getOrDefault("DEPO", "");
            String sTradeDate = pData.getOrDefault("TDDT", "");
            // String sValueDate = pData.getOrDefault("VLDT", "");
            String sTradeCcy = pData.getOrDefault("TCCY", "");
            String sInterestRate = pData.getOrDefault("IRTE", "");
            String sInterestDays = pData.getOrDefault("IDYS", "");
            String sIssueDate = pData.getOrDefault("ISDT", "");
            String sMaturityDate = pData.getOrDefault("MTDT", "");
            String sStockExchange = pData.getOrDefault("SEXC", "");

            // Customer-level fields
            String sCustomerNo = pData.getOrDefault("CUNO", "");
            String sPortfolioNo = pData.getOrDefault("PFNO", "");
            String sNominal = pData.getOrDefault("NOML", "");
            String sPrice = pData.getOrDefault("PRCE", "");
            String sGrossAmt = pData.getOrDefault("GAMT", "");
            String sNetAmount = pData.getOrDefault("NAMT", "");
            String sCuAccountNo = pData.getOrDefault("CUAC", "");

            // Broker-level fields
            String sBrokerNo = pData.getOrDefault("BRNO", "");
            String sBrAccountNo = pData.getOrDefault("DBAC", "");

            // Description
            // String sDescription = pData.getOrDefault("DESC", "");

            // Validate required fields
            if (sCustomerNo.isEmpty() || sSecurityNo.isEmpty() || sTradeCcy.isEmpty()) {
                yLOGGER.log(Level.SEVERE, LOG_PREFIX
                        + "buildStRecord: Missing required fields (CUSTOMER_NO, SECURITY_NO, or TRADE_CCY)");
                return false;
            }

            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "buildStRecord: Extracted fields - CUNO={0}, SENO={1}, TCCY={2}",
                    new Object[] { sCustomerNo, sSecurityNo, sTradeCcy });

            // Create SEC.TRADE record
            pSecTradeRecord = new SecTradeRecord();

            // === Main record fields ===
            pSecTradeRecord.setSecurityCode(sSecurityNo);
            pSecTradeRecord.setDepository(sDepository);
            pSecTradeRecord.setTradeDate(sTradeDate);
            pSecTradeRecord.setTradeCcy(sTradeCcy);
            pSecTradeRecord.setInterestRate(sInterestRate);
            pSecTradeRecord.setInterestDays(sInterestDays);
            pSecTradeRecord.setIssueDate(sIssueDate);
            pSecTradeRecord.setMaturityDate(sMaturityDate);
            pSecTradeRecord.setStockExchange(sStockExchange);

            // === CustomerNoClass (nested - index 0) ===
            CustomerNoClass pCustomer = new CustomerNoClass();
            pCustomer.setCustomerNo(sCustomerNo);
            pCustomer.setPortConstNo(sPortfolioNo);
            pCustomer.setCustAccNo(sCuAccountNo);
            pCustomer.setCuGrossAmTrd(sGrossAmt);
            pCustomer.setCuNetAmTrd(sNetAmount);

            // === CustNoNomClass (nested inside CustomerNoClass - nominal/price) ===
            CustNoNomClass pCustNom = new CustNoNomClass();
            pCustNom.setCustNoNom(sNominal);
            pCustNom.setCustPrice(sPrice);
            pCustomer.setCustNoNom(pCustNom, 0);

            // Add customer to record
            pSecTradeRecord.setCustomerNo(pCustomer, 0);

            // === BrokerNoClass (nested - index 0, if broker provided) ===
            if (!sBrokerNo.isEmpty()) {
                BrokerNoClass pBroker = new BrokerNoClass();
                pBroker.setBrokerNo(sBrokerNo);
                pBroker.setBrAccNo(sBrAccountNo);
                pSecTradeRecord.setBrokerNo(pBroker, 0);
            }

            // === TradeCurrClass (nested - trade currency details) ===
            TradeCurrClass pTradeCurr = new TradeCurrClass();
            pTradeCurr.setTradeCurr(sTradeCcy);
            pSecTradeRecord.addTradeCurr(pTradeCurr);

            yLOGGER.log(Level.INFO,
                    LOG_PREFIX + "buildStRecord: Record populated with nested classes");

            pTxnData = new SynchronousTransactionData();
            pTxnData.setResponseId(pResponseId);
            pTxnData.setVersionId(mOfsVersionSt);
            pTxnData.setFunction(mOfsFunction);
            pTxnData.setNumberOfAuthoriser("0");
            pTxnData.setSourceId(mOfsSource);
            pTxnData.setCompanyId(mCompanyId);

            pTransactionData.add(pTxnData);
            pRecords.add(pSecTradeRecord.toStructure());
            yLOGGER.log(Level.INFO, LOG_PREFIX
                    + "buildStRecord: SEC.TRADE record populated successfully with responseId={0}",
                    pResponseId);
            return true;

        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "buildStRecord: Error building SecTradeRecord",
                    e);
            return false;
        } finally {
            pSecTradeRecord = null;
            pTxnData = null;
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Builds response and publishes using CbnScPayload and CbnScProducer.
     */
    private void publishResponse(String pId, String pStatus, String pMessage, String pTransactRef,
            JsonNode pOriginalItem) {
        try {
            String pJsonResponse = mPayloadHandler.buildResponse(pStatus, pMessage, pTransactRef,
                    pOriginalItem);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "publishResponse: Built response: {0}",
                    pJsonResponse);
            mProducer.publishResponse(pJsonResponse, mAdapterFlag, pId);
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, e,
                    () -> LOG_PREFIX + "publishResponse: Error publishing response for id=" + pId);
        }
    }

    /**
     * Persists the failed transaction source to EXCEPTS directory.
     */
    private void persistToExcepts(String pId, String pReason) {
        try {
            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                FileItemRef pRef = CbnScAdapter.parseFileItemRef(pId);
                if (pRef != null) {
                    CbnScAdapter.persistFailedFileItem(pRef, mExceptsDir, pReason);
                } else {
                    yLOGGER.log(Level.WARNING,
                            LOG_PREFIX + "persistToExcepts: could not parse FILE ref from id={0}",
                            pId);
                }
            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                MqItemRef pRef = CbnScAdapter.parseMqItemRef(pId);
                if (pRef != null) {
                    CbnScAdapter.persistFailedMqItem(pRef, mExceptsDir, pReason);
                } else {
                    yLOGGER.log(Level.WARNING,
                            LOG_PREFIX + "persistToExcepts: could not parse WMQ ref from id={0}",
                            pId);
                }
            }
        } catch (Exception ex) {
            yLOGGER.log(Level.SEVERE, ex,
                    () -> LOG_PREFIX + "persistToExcepts: persistToExcepts failed for id=" + pId);
        }
    }

    /**
     * Extracts prefix from transaction ID for response identification (FILE mode fallback).
     */
    public static String extractPrefix(String pInput) {
        try {
            String[] pSegments = pInput.split("\\|");
            if (pSegments.length < 2) {
                return MSG_UNKNOWN;
            }
            String pFilePath = pSegments[1];
            int pLastSlash = Math.max(pFilePath.lastIndexOf('/'), pFilePath.lastIndexOf('\\'));
            String pFileName = pLastSlash >= 0 ? pFilePath.substring(pLastSlash + 1) : pFilePath;

            int pDashIndex = pFileName.indexOf('-');
            return pDashIndex > 0 ? pFileName.substring(0, pDashIndex) : pFileName;
        } catch (Exception e) {
            yLOGGER.log(Level.WARNING, e,
                    () -> LOG_PREFIX + "extractPrefix: Error extracting prefix from: " + pInput);
            return MSG_UNKNOWN;
        }
    }
}
