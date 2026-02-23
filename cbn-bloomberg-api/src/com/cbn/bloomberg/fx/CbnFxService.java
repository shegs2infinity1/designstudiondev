package com.cbn.bloomberg.fx;

import java.io.IOException;
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

import com.cbn.bloomberg.fx.CbnFxAdapter.FileItemRef;
import com.cbn.bloomberg.fx.CbnFxAdapter.MqItemRef;
import com.cbn.bloomberg.util.CbnTfBackup;
import com.cbn.bloomberg.util.CbnTfLogTracer;
import com.cbn.bloomberg.util.CbnTfProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temenos.api.TBoolean;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.forex.ForexRecord;
import com.temenos.t24.api.records.forex.OurAccountPayClass;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.cbnbksdrequest.CbnBksdRequestRecord;


/**
 * Title: CbnFxService.java
 * 
 * Author: CSD Development Team
 * 
 * Date Created: 2025-10-11
 * 
 * Last Modified: 2025-01-12
 *
 * Purpose: CBN Bloomberg FX Service Hook supporting dual ingestion modes (FILE/WMQ) with two-phase
 * processing:
 * 
 * 1. PROCESS.FX submits OFS requests
 * 
 * 2. CHECK.RESPONSE retrieves and publishes results.
 *
 * Architecture:
 * 
 * - Dual record types: ForexRecord (SP/FW/SW deals), CbnBksdRequestRecord (SD deals)
 * 
 * - Polymorphic record handling with type-safe transaction envelope creation
 * 
 * - SonarQube compliant: cognitive complexity <10, comprehensive error handling
 *
 * Modification History:
 * 
 * --------------------
 * 
 * 09/11/25 - Handle single FOREX_TRANSACTION object format
 * 
 * 12/01/26 - Decomposed buildForexRecord() into 6 focused methods (184→33 lines)
 * 
 * Methods: determineOfsVersion, buildFxRecord, applyDealTypeSpecifics, setIfPresent,
 * buildCbnRecord, addTransactionData Fixed NPE in polymorphic record.toStructure() handling
 * 
 * Reduced cognitive complexity 31→5 per method
 */

public class CbnFxService extends ServiceLifecycle {

    // ==== CONSTANTS ====
    private static final Logger yLogger = CbnTfLogTracer.forClass(
            CbnFxService.class);
    private static final CbnTfProperties CONFIG = CbnTfProperties.getInstance();

    // Control list constants
    private static final String CONTROL_PROCESS_FX = "PROCESS.FX";
    private static final String CONTROL_CHECK_RESPONSE = "CHECK.RESPONSE";
    
    // Message constants
    private static final String MSG_SUCCESS = "success";
    private static final String MSG_FAILURE = "failure";
    private static final String MSG_UNKNOWN = "UNKNOWN";

    // Log prefix
    private static final String LOG_PREFIX = "[CbnFxService] ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==== CONFIGURATION ==== WMQ Node
    private final String mAdapterFlag = CONFIG.getDefAdapter();
    private final String mOfsSource = CONFIG.getOfsSource();
    private final String mOfsFunction = CONFIG.getOfsFunction();
    private final String mOfsVersionDefault = CONFIG.getOfsVersionFxDef();
 
    // ==== CONFIGURATION ==== File Nodeqq
    private final String mFilePattern = CONFIG.getNfsFilePattern();
    private final Path mInboundDir = Paths.get(CONFIG.getNfsInboundDir());
    private final Path mProcessedDir = Paths.get(CONFIG.getNfsDoneDir());
    private final Path mExceptsDir = Paths.get(CONFIG.getNfsErrorDir());

    // ==== INSTANCE VARIABLES ====
    private Session mSession = null;
    private DataAccess mDataAccess = null;
    private final CbnFxPayloads mPayloadHandler = new CbnFxPayloads(OBJECT_MAPPER);
    private final CbnFxProducer mProducer = new CbnFxProducer();

    // ==== TRANSACTION METADATA CACHE ====
    // Maps responseId -> TransactionMetadata
    private static final Map<String, TransactionMetadata> TRANSACTION_CACHE = new HashMap<>();

    static {
        System.out.println("We are here now - CbnFxService class loaded");
    }

    /**
     * Metadata holder for tracking transaction state between phases.
     */
    private static class TransactionMetadata {

        String originalId;
        JsonNode originalItem;
        String adapterMode;
        String bloombergId;

        TransactionMetadata(String pOriginalId, JsonNode pOriginalItem, String pAdapterMode,String pBloombergId) {
            this.originalId = pOriginalId;
            this.originalItem = pOriginalItem;
            this.adapterMode = pAdapterMode;
            this.bloombergId = pBloombergId;
        }
    }

    /**
     * Initializes the service with session and data access objects.
     */
    @Override
    public void initialise(ServiceData serviceData) {
        
        System.out.println("We are here now - Initialise");
        try {
            yLogger.log(Level.INFO, LOG_PREFIX + "=== initialise() START ===");
            mSession = new Session(this);
            mDataAccess = new DataAccess(this);
            String mCompanyId = "";
            mCompanyId = mSession.getCompanyId();

            yLogger.log(Level.INFO, LOG_PREFIX + "initialise: Service initialized for company: {0}",
                    mCompanyId);
            yLogger.log(Level.INFO, LOG_PREFIX + "=== initialise() COMPLETE ===");
        } catch (T24CoreException e) {
            yLogger.log(Level.SEVERE, LOG_PREFIX + "initialise: Error initializing service", e);
        } catch (Exception e) {
            yLogger.log(Level.SEVERE,
                    LOG_PREFIX + "initialise: Unexpected error during initialization", e);
        }
    }
    

    /**
     * Retrieves the list of transaction IDs to be processed by the service. Implements two-phase
     * processing: - Phase 1: PROCESS.FX - Get new transactions from FILE/WMQ - Phase 2:
     * CHECK.RESPONSE - Get pending OFS responses
     */
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> pRecordIds = null;
        String pControlItem = null;
        System.out.println("We are here now - getID");
        try {
            yLogger.log(Level.INFO, LOG_PREFIX + "=== getIds() START ===");

            // Ensure initialization if not already done
            if (mSession == null || mDataAccess == null) {
                yLogger.log(Level.WARNING,
                        LOG_PREFIX + "getIds: Service not initialized, initializing now");
                initialise(serviceData);
            }

            // Initialize control list if empty - FIXED: Match test class pattern
            if (controlList == null || controlList.isEmpty()) {
                if (controlList == null) {
                    controlList = new ArrayList<>();
                }
                controlList.add(0, CONTROL_PROCESS_FX);
                controlList.add(1, CONTROL_CHECK_RESPONSE);
            }

            pControlItem = controlList.get(0);
            yLogger.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, adapterFlag={1}",
                    new Object[] { pControlItem, mAdapterFlag });

            // Process based on control item
            switch (pControlItem) {
                case CONTROL_PROCESS_FX:
                    pRecordIds = getNewTransactionIds();
                    break;

                case CONTROL_CHECK_RESPONSE:
                    pRecordIds = getPendingResponseIds();
                    break;

                default:
                    yLogger.log(Level.WARNING, LOG_PREFIX
                            + "getIds: Unrecognized controlList option: {0}. Returning empty list.",
                            pControlItem);
                    pRecordIds = Collections.emptyList();
                    break;
            }

            yLogger.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, total IDs={1}",
                    new Object[] { pControlItem, pRecordIds.size() });
            yLogger.log(Level.INFO, LOG_PREFIX + "=== getIds() COMPLETE ===");

        } catch (T24CoreException e) {
            yLogger.log(Level.SEVERE, LOG_PREFIX + "getIds: Error retrieving record IDs", e);
            pRecordIds = Collections.emptyList();
        } catch (Exception e) {
            yLogger.log(Level.SEVERE, LOG_PREFIX + "getIds: Unexpected error in getIds", e);
            pRecordIds = Collections.emptyList();
        } finally {
            // Cleanup local variables
            pControlItem = null;
        }
        return pRecordIds;
    }

    /**
     * Gets new transaction IDs from FILE or WMQ sources. NOW HANDLES SINGLE FOREX_TRANSACTION
     * OBJECT FORMAT
     */
    private List<String> getNewTransactionIds() {
        List<String> pIds = new ArrayList<>();

        try {
            yLogger.log(Level.INFO, LOG_PREFIX + "=== getNewTransactionIds() START ===");

            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                yLogger.log(Level.INFO,
                        LOG_PREFIX + "getNewTransactionIds: FILE mode: scanning {0}", mInboundDir);

                if (!Files.isDirectory(mInboundDir)) {
                    yLogger.log(Level.WARNING,
                            LOG_PREFIX + "getNewTransactionIds: Inbound directory not found: {0}",
                            mInboundDir);
                    return pIds;
                }

                pIds.addAll(CbnFxAdapter.scanDirectoryIds(mInboundDir, mFilePattern,
                        mProcessedDir, OBJECT_MAPPER));

            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                yLogger.log(Level.INFO,
                        LOG_PREFIX + "getNewTransactionIds: WMQ mode: consuming messages from MQ");
                pIds.addAll(CbnFxAdapter.extractIdsFromWmq(OBJECT_MAPPER));

            } else {
                yLogger.log(Level.SEVERE,
                        LOG_PREFIX + "getNewTransactionIds: Unknown adapter flag: {0}",
                        mAdapterFlag);
            }

            yLogger.log(Level.INFO,
                    LOG_PREFIX + "=== getNewTransactionIds() COMPLETE - Found {0} IDs ===",
                    pIds.size());

        } catch (Exception e) {
            yLogger.log(Level.SEVERE,
                    LOG_PREFIX + "getNewTransactionIds: Error retrieving new transaction IDs", e);
            pIds = Collections.emptyList();
        }

        return pIds;
    }

    /**
     * Gets pending OFS response IDs from the transaction cache.
     */
    private List<String> getPendingResponseIds() {
        List<String> pOfsResponseIds = new ArrayList<>();

        try {
            yLogger.log(Level.INFO, LOG_PREFIX + "=== getPendingResponseIds() START ===");

            synchronized (TRANSACTION_CACHE) {
                // Get all responseIds from cache
                pOfsResponseIds.addAll(TRANSACTION_CACHE.keySet());
            }

            yLogger.log(Level.INFO,
                    LOG_PREFIX + "getPendingResponseIds: Found {0} pending responses",
                    pOfsResponseIds.size());

        } catch (Exception e) {
            yLogger.log(Level.SEVERE,
                    LOG_PREFIX + "getPendingResponseIds: Error retrieving pending response IDs", e);
            pOfsResponseIds = Collections.emptyList();
        }

        return pOfsResponseIds;
    }

    /**
     * Updates a single record based on the current processing phase.
     */
    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        yLogger.log(Level.INFO, LOG_PREFIX + "=== updateRecord() START ===");
        yLogger.log(Level.INFO,
                LOG_PREFIX + "updateRecord: Processing updateRecord for controlItem: {0}, ID: {1}",
                new Object[] { controlItem, id });

        try {
            // Ensure initialization if not already done
            if (mSession == null || mDataAccess == null) {
                yLogger.log(Level.WARNING,
                        LOG_PREFIX + "updateRecord: Service not initialized, initializing now");
                initialise(serviceData);
            }

            // FIXED: Added null/empty check for controlItem
            if (controlItem == null || controlItem.isEmpty()) {
                yLogger.log(Level.WARNING, LOG_PREFIX
                        + "updateRecord: Control item is null or empty, defaulting to PROCESS.FX");
                controlItem = CONTROL_PROCESS_FX;
            }

            // Process based on control item
            switch (controlItem) {
                case CONTROL_PROCESS_FX:
                    processOfsRequest(id, transactionData, records);
                    break;

                case CONTROL_CHECK_RESPONSE:
                    checkOfsResponse(id);
                    break;

                default:
                    yLogger.log(Level.WARNING,
                            LOG_PREFIX + "updateRecord: Unrecognized control item: {0}",
                            controlItem);
                    break;
            }

            yLogger.log(Level.INFO, LOG_PREFIX + "=== updateRecord() COMPLETE ===");

        } catch (T24CoreException e) {
            yLogger.log(Level.SEVERE, LOG_PREFIX + "updateRecord: Error updating record", e);
        } catch (Exception e) {
            yLogger.log(Level.SEVERE, LOG_PREFIX + "updateRecord: Unexpected error in updateRecord",
                    e);
        }
    }

    /**
     * Phase 1: Process OFS request - retrieve transaction, map to FX record, submit to T24. NOW
     * HANDLES SINGLE FOREX_TRANSACTION OBJECT FORMAT
     */
    private void processOfsRequest(String pRecordId,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        JsonNode pOriginalItem = null;
        String pOfsResponseId = null;
        String bloombergId = null;

        try {
            yLogger.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() START ===");
            yLogger.log(Level.INFO,
                    LOG_PREFIX + "processOfsRequest: Processing OFS request for record ID: {0}",
                    pRecordId);

            // Step 1: Retrieve the item based on adapter mode
            JsonNode pItem = retrieveTransactionItem(pRecordId);
            CbnTfBackup.backupMessage(pItem.toString(), "FOREX_TRANSACTION", pRecordId);
            bloombergId = pItem.path("BLOOMBERG_ID").asText("");
            if (pItem == null) {
                pMessage = "Item not found or invalid FOREX_TRANSACTION";
                yLogger.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", null,bloombergId);
                return;
            }

            pOriginalItem = pItem;
            yLogger.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Retrieved item for id={0}",
                    pRecordId);

            // Step 2: Map JSON to FX field map
            Map<String, String> pData = CbnFxMapping.mapForexTransactToFx(pItem);
            if (pData == null || pData.isEmpty()) {
                pMessage = "Mapping returned no data";
                yLogger.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);
                return;
            }

            yLogger.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Mapped data for id={0}",
                    pRecordId);

            // Step 3: Build responseId
            pOfsResponseId = buildForexResponseId(pRecordId);
            yLogger.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Built responseId: {0}",
                    pOfsResponseId);

            // Step 4: Build and populate FX record
            boolean pSuccess = buildForexRecord(pOfsResponseId, pData, pTransactionData, pRecords);
            if (!pSuccess) {
                pMessage = "Validation failed while building ForexRecord";
                yLogger.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);
                return;
            }

            yLogger.log(Level.INFO, LOG_PREFIX
                    + "processOfsRequest: Successfully prepared OFS transaction for FX update");

            // Step 5: Store transaction metadata in cache for Phase 2
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.put(pOfsResponseId,
                        new TransactionMetadata(pRecordId, pOriginalItem, mAdapterFlag,bloombergId));
            }

            yLogger.log(Level.INFO,
                    LOG_PREFIX
                            + "processOfsRequest: Transaction metadata cached for responseId={0}",
                    pOfsResponseId);
            yLogger.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() COMPLETE ===");

        } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
            yLogger.log(Level.SEVERE, jpe,
                    () -> LOG_PREFIX + "processOfsRequest: JSON parse error for id=" + pRecordId);
            pMessage = "JSON parse error: " + jpe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);

        } catch (IOException ioe) {
            yLogger.log(Level.SEVERE, ioe,
                    () -> LOG_PREFIX + "processOfsRequest: I/O error for id=" + pRecordId);
            pMessage = "I/O error: " + ioe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);

        } catch (RuntimeException re) {
            yLogger.log(Level.SEVERE, re,
                    () -> LOG_PREFIX + "processOfsRequest: Runtime error for id=" + pRecordId);
            pMessage = "Runtime error: " + re.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);
        } finally {
            // Cleanup
            pOriginalItem = null;
            pOfsResponseId = null;
        }
    }

    /**
     * Phase 2: Check OFS response - retrieve response from OFS.REQUEST.DETAIL and publish to MQ.
     */
    private void checkOfsResponse(String pOfsResponseId) {
        OfsRequestDetailRecord pOfsRequestDetailRecord = null;
        TBoolean pExists = null;
        String pMsgOut = null;
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        String pTransactRef = "";

        yLogger.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() START ===");

        // Retrieve transaction metadata from cache
        TransactionMetadata pMetadata;
        synchronized (TRANSACTION_CACHE) {
            pMetadata = TRANSACTION_CACHE.get(pOfsResponseId);
        }

        if (pMetadata == null) {
            yLogger.log(Level.WARNING,
                    LOG_PREFIX + "checkOfsResponse: No metadata found for responseId={0}",
                    pOfsResponseId);
            return;
        }

        try {
            yLogger.log(Level.INFO,
                    LOG_PREFIX + "checkOfsResponse: Checking OFS response for record ID: {0}",
                    pOfsResponseId);

            // Initialize TBoolean with null - same pattern as test class
            pExists = new TBoolean(null);
            pOfsRequestDetailRecord = mDataAccess.getRequestResponse(pOfsResponseId, pExists);
            yLogger.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: OFS record exists: {0}",
                    pExists);

            // Check if record exists and has message output
            if (pOfsRequestDetailRecord != null && pOfsRequestDetailRecord.getMsgOut() != null) {
                pMsgOut = pOfsRequestDetailRecord.getMsgOut().getValue();

                if (pMsgOut != null && !pMsgOut.isEmpty()) {
                    yLogger.log(Level.INFO, LOG_PREFIX
                            + "checkOfsResponse: OFS response message retrieved successfully");
                    yLogger.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: ORD OUT MSG retrieved");

                    // Check for OFS errors
                    if (pMsgOut.contains("/-1/")) {
                        pStatus = MSG_FAILURE;
                        pMessage = "T24 OFS Error: " + pMsgOut;
                        pTransactRef = "ERROR";
                        yLogger.log(Level.WARNING,
                                LOG_PREFIX + "checkOfsResponse: OFS Error detected: {0}", pMsgOut);

                    } else {
                        // Parse transaction reference from OFS response
                        pTransactRef = pOfsRequestDetailRecord.getTransReference().toString();
                        pStatus = MSG_SUCCESS;
                        pMessage = "Forex transaction processed successfully";
                        yLogger.log(Level.INFO,
                                LOG_PREFIX + "checkOfsResponse: Transaction successful: ref={0}",
                                pTransactRef);
                    }

                    // Publish response
                    publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                            pMetadata.originalItem, pMetadata.bloombergId);

                    // Acknowledge MQ message on success
                    if (MSG_SUCCESS.equalsIgnoreCase(pStatus)
                            && "WMQ".equalsIgnoreCase(pMetadata.adapterMode)) {
                        CbnFxAdapter.acknowledgeMqMessage(pMetadata.originalId);
                        yLogger.log(Level.INFO,
                                LOG_PREFIX + "checkOfsResponse: MQ message acknowledged for id={0}",
                                pMetadata.originalId);
                    }

                    // Remove from cache after successful processing
                    synchronized (TRANSACTION_CACHE) {
                        TRANSACTION_CACHE.remove(pOfsResponseId);
                    }

                } else {
                    yLogger.log(Level.WARNING,
                            LOG_PREFIX + "checkOfsResponse: OFS message output is null or empty");
                }

            } else {
                yLogger.log(Level.WARNING, LOG_PREFIX
                        + "checkOfsResponse: OFS request detail record or message output is null");
            }

            yLogger.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() COMPLETE ===");

        } catch (T24CoreException e) {
            yLogger.log(Level.SEVERE, LOG_PREFIX + "checkOfsResponse: Error checking OFS response",
                    e);
            pMessage = "Error checking OFS response: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                    pMetadata.originalItem,pMetadata.bloombergId);

            // Remove from cache on error
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.remove(pOfsResponseId);
            }

        } catch (Exception e) {
            yLogger.log(Level.SEVERE,
                    LOG_PREFIX + "checkOfsResponse: Unexpected error in checkOfsResponse", e);
            pMessage = "Unexpected error: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                    pMetadata.originalItem,pMetadata.bloombergId);

            // Remove from cache on error
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.remove(pOfsResponseId);
            }

        } finally {
            pOfsRequestDetailRecord = null;
            pExists = null;
            pMsgOut = null;
        }
    }

    /**
     * Retrieves transaction item from FILE or WMQ based on adapter mode. NOW HANDLES SINGLE
     * FOREX_TRANSACTION OBJECT FORMAT
     */
    private JsonNode retrieveTransactionItem(String pId) throws IOException {
        yLogger.log(Level.INFO, LOG_PREFIX + "=== retrieveTransactionItem() START for ID: {0} ===",
                pId);

        JsonNode result = null;
        if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            result = processFileRecord(pId);
        } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            result = processWmqRecord(pId);
        } else {
            yLogger.log(Level.SEVERE,
                    LOG_PREFIX + "retrieveTransactionItem: Unknown adapter flag: {0}",
                    mAdapterFlag);
        }

        yLogger.log(Level.INFO, LOG_PREFIX + "=== retrieveTransactionItem() COMPLETE ===");
        return result;
    }

    /**
     * Processes a FILE mode record.
     */
    private JsonNode processFileRecord(String pId) throws IOException {
        FileItemRef pRef = CbnFxAdapter.parseFileItemRef(pId);
        if (pRef == null) {
            yLogger.log(Level.WARNING,
                    LOG_PREFIX + "processFileRecord: FILE mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CbnFxAdapter.readRoot(pRef.file(), OBJECT_MAPPER);
        if (!CbnFxMapping.hasForexTransacts(pRoot)) {
            yLogger.log(Level.WARNING,
                    LOG_PREFIX + "processFileRecord: FILE mode: no FOREX_TRANSACTION in {0}",
                    pRef.file().getFileName());
            return null;
        }

        return CbnFxMapping.getForexTransactAt(pRoot, pRef.index());
    }

    /**
     * Processes a WMQ mode record.
     */
    private JsonNode processWmqRecord(String pId) throws IOException {
        MqItemRef pRef = CbnFxAdapter.parseMqItemRef(pId);
        if (pRef == null) {
            yLogger.log(Level.WARNING,
                    LOG_PREFIX + "processWmqRecord: WMQ mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CbnFxAdapter.readMqMessage(pRef.messageId(), OBJECT_MAPPER);
        if (pRoot == null || !CbnFxMapping.hasForexTransacts(pRoot)) {
            yLogger.log(Level.WARNING,
                    LOG_PREFIX + "processWmqRecord: WMQ mode: no FOREX_TRANSACTION in message {0}",
                    pRef.messageId());
            return null;
        }

        return CbnFxMapping.getForexTransactAt(pRoot, pRef.index());
    }

    /**
     * Builds the responseId for T24 based on the adapter mode.
     */
    private String buildForexResponseId(String pId) {
        if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            MqItemRef pRef = CbnFxAdapter.parseMqItemRef(pId);
            if (pRef != null && pRef.messageId() != null) {
                String pJmsMessageId = pRef.messageId();

                yLogger.log(Level.INFO,
                        LOG_PREFIX
                                + "buildForexResponseId: Using JMS Message ID as responseId: {0}",
                        pJmsMessageId);
                return pJmsMessageId;
            }
        } else if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            return extractPrefix(pId);
        }
        return extractPrefix(pId);
    }

    /**
     * Builds a ForexRecord from the mapped data and populates the transaction lists.
     */
    private boolean buildForexRecord(String pOfsResponseId, Map<String, String> pData,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {

        try {
            yLogger.log(Level.INFO, LOG_PREFIX + "=== buildForexRecord() START ===");
            yLogger.log(Level.INFO,
                    LOG_PREFIX + "buildForexRecord: Building forex record for responseId: {0}",
                    pOfsResponseId);

            String sDealType = pData.getOrDefault("DTYP", "");
            String pOfsVersion = determineOfsVersion(sDealType);

            if (isBksdForexDeal(sDealType)) {
                CbnBksdRequestRecord pCbnBksdRecord = buildBksdForexRecord(pData);
                addTransactionData(pOfsResponseId, pOfsVersion, pTransactionData, pRecords,
                        pCbnBksdRecord);
                yLogger.log(Level.INFO,
                        LOG_PREFIX + "buildForexRecord: CBN BKSD record created for deal type SD");
            } else if (isCoreForexDeal(sDealType)) {
                ForexRecord pFxRecord = buildCoreForexRecord(pData, sDealType);
                addTransactionData(pOfsResponseId, pOfsVersion, pTransactionData, pRecords,
                        pFxRecord);
                yLogger.log(Level.INFO,
                        LOG_PREFIX + "buildForexRecord: Forex record created for deal type {0}",
                        sDealType);
            } else {
                yLogger.log(Level.SEVERE, LOG_PREFIX + "buildForexRecord: Unknown deal type: {0}",
                        sDealType);
                return false;
            }

            yLogger.log(Level.INFO, LOG_PREFIX
                    + "buildForexRecord: Record populated successfully, transaction data added with responseId={0}",
                    pOfsResponseId);
            return true;

        } catch (Exception e) {
            yLogger.log(Level.SEVERE, LOG_PREFIX + "buildForexRecord: Error building record", e);
            return false;
        }
    }

    /**
     * Checks if deal type is a CBN BKSD deal (SD).
     */
    private boolean isBksdForexDeal(String dealType) {
        return "SD".equalsIgnoreCase(dealType);
    }

    /**
     * Checks if deal type is a standard Forex deal (FW/SP/SW).
     */
    private boolean isCoreForexDeal(String dealType) {
        return "FW".equalsIgnoreCase(dealType) || "SP".equalsIgnoreCase(dealType)
                || "SW".equalsIgnoreCase(dealType);
    }

    /**
     * Determines the OFS version based on deal type.
     */
    private String determineOfsVersion(String sDealType) {
        String pOfsVersion = CONFIG.getOfsVersionFx(sDealType);
        if (pOfsVersion == null || pOfsVersion.isEmpty()) {
            pOfsVersion = mOfsVersionDefault;
        }
        yLogger.log(Level.INFO, LOG_PREFIX + "buildForexRecord: Using OFS Version: {0}",
                pOfsVersion);
        return pOfsVersion;
    }

    /**
     * Builds a ForexRecord from mapped data.
     */
    private ForexRecord buildCoreForexRecord(Map<String, String> pData, String sDealType) {
        ForexRecord pFxRecord = new ForexRecord();

        // Core fields
        pFxRecord.setCounterparty(pData.getOrDefault("CPTY", ""));
        pFxRecord.setDealerDesk(pData.getOrDefault("DESK", ""));
        pFxRecord.setDealDate(pData.getOrDefault("DDAT", ""));
        pFxRecord.setCurrencyBought(pData.getOrDefault("CCYB", ""));
        pFxRecord.setAmountBought(pData.getOrDefault("BAMT", ""));
        pFxRecord.setValueDateBuy(pData.getOrDefault("VBUY", ""));
        pFxRecord.setCurrencySold(pData.getOrDefault("CCYS", ""));
        pFxRecord.setAmountSold(pData.getOrDefault("SAMT", ""));
        pFxRecord.setValueDateSell(pData.getOrDefault("VSEL", ""));
        pFxRecord.setSpotRate(pData.getOrDefault("SPRT", ""));
        pFxRecord.setSpotDate(pData.getOrDefault("SPDT", ""));
        pFxRecord.setBaseCcy(pData.getOrDefault("BCCY", ""));
        pFxRecord.setBroker(pData.getOrDefault("BRKR", ""));
        pFxRecord.setCpyCorrName(pData.getOrDefault("CCNO", ""));
        pFxRecord.setCpyCorrCity(pData.getOrDefault("CCAD", ""));
        pFxRecord.setAccountToCharge(pData.getOrDefault("CBNK", ""));
        pFxRecord.setIntRateBuy(pData.getOrDefault("INTB", ""));
        pFxRecord.setIntRateSell(pData.getOrDefault("INTS", ""));

        OurAccountPayClass oAcctPayClass = new OurAccountPayClass();
        oAcctPayClass.setOurAccountRec(pData.getOrDefault("OACR", ""));
        oAcctPayClass.setOurAccountPay(pData.getOrDefault("OACP", ""));
        pFxRecord.setOurAccountPay(oAcctPayClass, 0);

        applyDealTypeSpecifics(pFxRecord, pData, sDealType);
        return pFxRecord;
    }

    /**
     * Applies deal-type specific fields to the ForexRecord.
     */
    private void applyDealTypeSpecifics(ForexRecord pFxRecord, Map<String, String> pData,
            String sDealType) {
        if (sDealType == null || sDealType.trim().isEmpty()) {
            return;
        }

        String normalizedType = sDealType.trim().toUpperCase();

        switch (normalizedType) {
            case "FW":
                String sForwardRate = pData.getOrDefault("FWRT", "");
                if (!sForwardRate.isEmpty()) {
                    yLogger.log(Level.FINE,
                            LOG_PREFIX + "buildForexRecord: FW: set FORWARD_RATE={0}",
                            sForwardRate);
                }
                break;

            case "SW":
                setIfPresent(pFxRecord::setSwapBaseCcy, pData, "SCCY");
                setIfPresent(pFxRecord::setLeg1FwdRate, pData, "LG1R");
                setIfPresent(pFxRecord::setForwardRate, pData, "FWRT");
                setIfPresent(pFxRecord::setFwdFwdSwap, pData, "FFWS");
                setIfPresent(pFxRecord::setUnevenSwap, pData, "UEVS");

                String sSwapRef1 = pData.getOrDefault("SRF1", "");
                String sSwapRef2 = pData.getOrDefault("SRF2", "");
                if (!sSwapRef1.isEmpty()) {
                    pFxRecord.setSwapRefNo(sSwapRef1, 0);
                }
                if (!sSwapRef2.isEmpty()) {
                    pFxRecord.setSwapRefNo(sSwapRef2, 1);
                }
                yLogger.log(Level.FINE,
                        LOG_PREFIX + "buildForexRecord: SW: set swap-specific fields");
                break;

            case "SP":
                yLogger.log(Level.FINE,
                        LOG_PREFIX + "buildForexRecord: SP: no deal-specific fields to set");
                break;

            default:
                yLogger.log(Level.WARNING, LOG_PREFIX + "buildForexRecord: Unknown DEAL_TYPE: {0}",
                        sDealType);
        }
    }

    /**
     * Helper to set field value if present in data map.
     */
    private void setIfPresent(java.util.function.Consumer<String> setter, Map<String, String> pData,
            String key) {
        String value = pData.getOrDefault(key, "");
        if (!value.isEmpty()) {
            setter.accept(value);
        }
    }

    /**
     * Builds a CbnBksdRequestRecord from mapped data.
     */
    private CbnBksdRequestRecord buildBksdForexRecord(Map<String, String> pData) {
        CbnBksdRequestRecord pCbnBksdRecord = new CbnBksdRequestRecord();
        
        // Local App Fields
        //pCbnBksdRecord.setDealType(pData.getOrDefault("DTYP", ""));
        pCbnBksdRecord.setDealType("SPOT"); // TO substitute the SD type to SPOT transaction
        pCbnBksdRecord.setCounterparty(pData.getOrDefault("CPTY", ""));
        pCbnBksdRecord.setDealDate(pData.getOrDefault("DDAT", ""));
        pCbnBksdRecord.setCurrencyBought(pData.getOrDefault("CCYB", ""));
        pCbnBksdRecord.setBuyAmount(pData.getOrDefault("BAMT", ""));
        pCbnBksdRecord.setValueDateBuy(pData.getOrDefault("VBUY", ""));
        pCbnBksdRecord.setCurrencySold(pData.getOrDefault("CCYS", ""));
        pCbnBksdRecord.setSellAmount(pData.getOrDefault("SAMT", ""));
        pCbnBksdRecord.setValueDateSell(pData.getOrDefault("VSEL", ""));
        pCbnBksdRecord.setDealerNotes(pData.getOrDefault("NOTS", ""));
        pCbnBksdRecord.setRate(pData.getOrDefault("DRTE", ""));
        pCbnBksdRecord.setPayRecAccount(pData.getOrDefault("PACC", ""));
        pCbnBksdRecord.setStatus(pData.getOrDefault("STSS", ""));
        pCbnBksdRecord.setRtgsId(pData.getOrDefault("RTGS", ""));
        pCbnBksdRecord.setCompletedId(pData.getOrDefault("IDCM", ""));
        pCbnBksdRecord.setBuyRate(pData.getOrDefault("BUYR", ""));
        pCbnBksdRecord.setSellRate(pData.getOrDefault("SELR", ""));
        pCbnBksdRecord.setMidRate(pData.getOrDefault("MRTE", ""));
        pCbnBksdRecord.setMdcError(pData.getOrDefault("MERR", ""));
        return pCbnBksdRecord;
    }

    /**
     * Creates transaction data and adds record to lists.
     */
    private void addTransactionData(String pOfsResponseId, String pOfsVersion,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords,
            Object record) {
        SynchronousTransactionData pTxnData = new SynchronousTransactionData();
        pTxnData.setResponseId(pOfsResponseId);
        pTxnData.setVersionId(pOfsVersion);
        pTxnData.setFunction(mOfsFunction);
        pTxnData.setNumberOfAuthoriser("0");
        pTxnData.setSourceId(mOfsSource);
        pTxnData.setCompanyId("NG0010001");

        pTransactionData.add(pTxnData);
        if (record instanceof ForexRecord) {
            pRecords.add(((ForexRecord) record).toStructure());
        } else if (record instanceof CbnBksdRequestRecord) {
            pRecords.add(((CbnBksdRequestRecord) record).toStructure());
        }
    }

    /**
     * Builds response and publishes using CbnFxPayloads and CbnFxProducer.
     */
    private void publishResponse(String pId, String pStatus, String pMessage, String pTransactRef,
            JsonNode pOriginalItem, String bloombergId) {
        try {
            String pJsonResponse = mPayloadHandler.buildResponse(pStatus, pMessage, pTransactRef,
                    pOriginalItem,bloombergId);
            yLogger.log(Level.INFO, LOG_PREFIX + "publishResponse: Built response: {0}",
                    pJsonResponse);
            CbnTfBackup.backupMessage(pJsonResponse, "FOREX_TRANSACTION", pTransactRef);
            mProducer.publishResponse(pJsonResponse, mAdapterFlag, pId);
        } catch (Exception e) {
            yLogger.log(Level.SEVERE, e,
                    () -> LOG_PREFIX + "publishResponse: Error publishing response for id=" + pId);
        }
    }

    /**
     * Persists the failed transaction source to EXCEPTS directory.
     */
    private void persistToExcepts(String pId, String pReason) {
        try {
            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                FileItemRef pRef = CbnFxAdapter.parseFileItemRef(pId);
                if (pRef != null) {
                    CbnFxAdapter.persistFailedFileItem(pRef, mExceptsDir, pReason);
                } else {
                    yLogger.log(Level.WARNING,
                            LOG_PREFIX + "persistToExcepts: could not parse FILE ref from id={0}",
                            pId);
                }
            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                MqItemRef pRef = CbnFxAdapter.parseMqItemRef(pId);
                if (pRef != null) {
                    CbnFxAdapter.persistFailedMqItem(pRef, mExceptsDir, pReason);
                } else {
                    yLogger.log(Level.WARNING,
                            LOG_PREFIX + "persistToExcepts: could not parse WMQ ref from id={0}",
                            pId);
                }
            }
        } catch (Exception ex) {
            yLogger.log(Level.SEVERE, ex,
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
            yLogger.log(Level.WARNING, e,
                    () -> LOG_PREFIX + "extractPrefix: Error extracting prefix from: " + pInput);
            return MSG_UNKNOWN;
        }
    }
}