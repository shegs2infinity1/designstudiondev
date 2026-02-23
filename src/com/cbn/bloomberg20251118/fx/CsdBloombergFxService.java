package com.cbn.bloomberg.fx;

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

import com.cbn.bloomberg.fx.CsdBloombergFxAdapter.FileItemRef;
import com.cbn.bloomberg.fx.CsdBloombergFxAdapter.MqItemRef;
import com.cbn.bloomberg.hp.CsdBloombergProperties;
import com.cbn.bloomberg.hp.CsdBloombergTafjLogger;
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

/**
 * Title: CsdBloombergFxService.java Author: CSD Development Team Date Created:
 * 2025-10-11 Last Modified: 2025-11-09
 *
 * Purpose: Bloomberg FX Service Hook supporting dual ingestion modes: FILE or
 * WMQ (IBM MQ). The adapter mode is controlled by bloomberg.properties.
 *
 * Two-Phase Processing Pattern: - Phase 1 (PROCESS.FX): Submit OFS request to
 * T24 with responseId - Phase 2 (CHECK.RESPONSE): Retrieve OFS response from
 * OFS.REQUEST.DETAIL and publish to MQ
 *
 * This approach ensures T24 has time to process the OFS request before
 * attempting to retrieve the response.
 *
 * Modification Details: ---- 09/11/25 - Updated to handle single
 * FOREX_TRANSACTION object format
 */

public class CsdBloombergFxService extends ServiceLifecycle {

    // ==== CONSTANTS ====
    private static final Logger LOG = CsdBloombergTafjLogger.forClass(CsdBloombergFxService.class);
    private static final CsdBloombergProperties CONFIG = CsdBloombergProperties.getInstance();

    // Control list constants
    private static final String CONTROL_PROCESS_FX = "PROCESS.FX";
    private static final String CONTROL_CHECK_RESPONSE = "CHECK.RESPONSE";

    // Message constants
    private static final String MSG_SUCCESS = "success";
    private static final String MSG_FAILURE = "failure";
    private static final String MSG_UNKNOWN = "UNKNOWN";

    // Log prefix
    private static final String LOG_PREFIX = "[CsdBloombergFxService] ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==== CONFIGURATION ====
    private final String mAdapterFlag = CONFIG.getFxAdapterMode();
    private final String mOfsFunction = CONFIG.getFxOfsFunction();
    private final String mOfsSource = CONFIG.getFxOfsSource();
    private final String mOfsVersionDefault = CONFIG.getFxOfsVersionDefault();
    private final String mFilePattern = CONFIG.getFxFilePattern();
    private final Path mInboundDir = Paths.get(CONFIG.geFxtInboundDir());
    private final Path mProcessedDir = Paths.get(CONFIG.getFxDoneDir());
    private final Path mExceptsDir = Paths.get(CONFIG.getFxErrorDir());

    // ==== INSTANCE VARIABLES ====
    private String mCompanyId = "";
    private Session mSession = null;
    private DataAccess mDataAccess = null;
    private final CsdBloombergFxPayload mPayloadHandler = new CsdBloombergFxPayload(OBJECT_MAPPER);
    private final CsdBloombergFxProducer mProducer = new CsdBloombergFxProducer();

    // ==== TRANSACTION METADATA CACHE ====
    // Maps responseId -> TransactionMetadata
    private static final Map<String, TransactionMetadata> TRANSACTION_CACHE = new HashMap<>();

    /**
     * Metadata holder for tracking transaction state between phases.
     */
    private static class TransactionMetadata {
        String originalId;
        JsonNode originalItem;
        String adapterMode;

        TransactionMetadata(String pOriginalId, JsonNode pOriginalItem, String pAdapterMode) {
            this.originalId = pOriginalId;
            this.originalItem = pOriginalItem;
            this.adapterMode = pAdapterMode;
        }
    }

    /**
     * Initializes the service with session and data access objects.
     */
    @Override
    public void initialise(ServiceData serviceData) {
        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== initialise() START ===");
            mSession = new Session(this);
            mDataAccess = new DataAccess(this);
            mCompanyId = mSession.getCompanyId();
            LOG.log(Level.INFO, LOG_PREFIX + "initialise: Service initialized for company: {0}", mCompanyId);
            LOG.log(Level.INFO, LOG_PREFIX + "=== initialise() COMPLETE ===");
        } catch (T24CoreException e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "initialise: Error initializing service", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "initialise: Unexpected error during initialization", e);
        }
    }

    /**
     * Retrieves the list of transaction IDs to be processed by the service.
     * Implements two-phase processing: - Phase 1: PROCESS.FX - Get new transactions
     * from FILE/WMQ - Phase 2: CHECK.RESPONSE - Get pending OFS responses
     */
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> pRecordIds = null;
        String pControlItem = null;

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== getIds() START ===");

            // Ensure initialization if not already done
            if (mSession == null || mDataAccess == null) {
                LOG.log(Level.WARNING, LOG_PREFIX + "getIds: Service not initialized, initializing now");
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
            LOG.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, adapterFlag={1}",
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
                LOG.log(Level.WARNING,
                        LOG_PREFIX + "getIds: Unrecognized controlList option: {0}. Returning empty list.",
                        pControlItem);
                pRecordIds = Collections.emptyList();
                break;
            }

            LOG.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, total IDs={1}",
                    new Object[] { pControlItem, pRecordIds.size() });
            LOG.log(Level.INFO, LOG_PREFIX + "=== getIds() COMPLETE ===");

        } catch (T24CoreException e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "getIds: Error retrieving record IDs", e);
            pRecordIds = Collections.emptyList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "getIds: Unexpected error in getIds", e);
            pRecordIds = Collections.emptyList();
        } finally {
            // Cleanup local variables
            pControlItem = null;
        }
        return pRecordIds;
    }

    /**
     * Gets new transaction IDs from FILE or WMQ sources. NOW HANDLES SINGLE
     * FOREX_TRANSACTION OBJECT FORMAT
     */
    private List<String> getNewTransactionIds() {
        List<String> pIds = new ArrayList<>();

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== getNewTransactionIds() START ===");

            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                LOG.log(Level.INFO, LOG_PREFIX + "getNewTransactionIds: FILE mode: scanning {0}", mInboundDir);

                if (!Files.isDirectory(mInboundDir)) {
                    LOG.log(Level.WARNING, LOG_PREFIX + "getNewTransactionIds: Inbound directory not found: {0}",
                            mInboundDir);
                    return pIds;
                }

                pIds.addAll(CsdBloombergFxAdapter.extractIdsFromDirectoryAndMove(mInboundDir, mFilePattern,
                        mProcessedDir, OBJECT_MAPPER));

            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                LOG.log(Level.INFO, LOG_PREFIX + "getNewTransactionIds: WMQ mode: consuming messages from MQ");
                pIds.addAll(CsdBloombergFxAdapter.extractIdsFromMq(OBJECT_MAPPER));

            } else {
                LOG.log(Level.SEVERE, LOG_PREFIX + "getNewTransactionIds: Unknown adapter flag: {0}", mAdapterFlag);
            }

            LOG.log(Level.INFO, LOG_PREFIX + "=== getNewTransactionIds() COMPLETE - Found {0} IDs ===", pIds.size());

        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "getNewTransactionIds: Error retrieving new transaction IDs", e);
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
            LOG.log(Level.INFO, LOG_PREFIX + "=== getPendingResponseIds() START ===");

            synchronized (TRANSACTION_CACHE) {
                // Get all responseIds from cache
                pResponseIds.addAll(TRANSACTION_CACHE.keySet());
            }

            LOG.log(Level.INFO, LOG_PREFIX + "getPendingResponseIds: Found {0} pending responses", pResponseIds.size());

        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "getPendingResponseIds: Error retrieving pending response IDs", e);
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

        LOG.log(Level.INFO, LOG_PREFIX + "=== updateRecord() START ===");
        LOG.log(Level.INFO, LOG_PREFIX + "updateRecord: Processing updateRecord for controlItem: {0}, ID: {1}",
                new Object[] { controlItem, id });

        try {
            // Ensure initialization if not already done
            if (mSession == null || mDataAccess == null) {
                LOG.log(Level.WARNING, LOG_PREFIX + "updateRecord: Service not initialized, initializing now");
                initialise(serviceData);
            }

            // FIXED: Added null/empty check for controlItem
            if (controlItem == null || controlItem.isEmpty()) {
                LOG.log(Level.WARNING,
                        LOG_PREFIX + "updateRecord: Control item is null or empty, defaulting to PROCESS.FX");
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
                LOG.log(Level.WARNING, LOG_PREFIX + "updateRecord: Unrecognized control item: {0}", controlItem);
                break;
            }

            LOG.log(Level.INFO, LOG_PREFIX + "=== updateRecord() COMPLETE ===");

        } catch (T24CoreException e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "updateRecord: Error updating record", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "updateRecord: Unexpected error in updateRecord", e);
        }
    }

    /**
     * Phase 1: Process OFS request - retrieve transaction, map to FX record, submit
     * to T24. NOW HANDLES SINGLE FOREX_TRANSACTION OBJECT FORMAT
     */
    private void processOfsRequest(String pRecordId, List<SynchronousTransactionData> pTransactionData,
            List<TStructure> pRecords) {
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        JsonNode pOriginalItem = null;
        String pResponseId = null;

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() START ===");
            LOG.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Processing OFS request for record ID: {0}", pRecordId);

            // Step 1: Retrieve the item based on adapter mode
            JsonNode pItem = retrieveTransactionItem(pRecordId);
            if (pItem == null) {
                pMessage = "Item not found or invalid FOREX_TRANSACTION";
                LOG.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", null);
                return;
            }

            pOriginalItem = pItem;
            LOG.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Retrieved item for id={0}", pRecordId);

            // Step 2: Map JSON to FX field map
            Map<String, String> pData = CsdBloombergFxMapper.mapForexTransactToFx(pItem);
            if (pData == null || pData.isEmpty()) {
                pMessage = "Mapping returned no data";
                LOG.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
                return;
            }

            LOG.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Mapped data for id={0}", pRecordId);

            // Step 3: Build responseId
            pResponseId = buildResponseId(pRecordId);
            LOG.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Built responseId: {0}", pResponseId);

            // Step 4: Build and populate FX record
            boolean pSuccess = buildForexRecord(pResponseId, pData, pTransactionData, pRecords);
            if (!pSuccess) {
                pMessage = "Validation failed while building ForexRecord";
                LOG.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
                return;
            }

            LOG.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Successfully prepared OFS transaction for FX update");

            // Step 5: Store transaction metadata in cache for Phase 2
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.put(pResponseId, new TransactionMetadata(pRecordId, pOriginalItem, mAdapterFlag));
            }

            LOG.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Transaction metadata cached for responseId={0}",
                    pResponseId);
            LOG.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() COMPLETE ===");

        } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
            LOG.log(Level.SEVERE, jpe, () -> LOG_PREFIX + "processOfsRequest: JSON parse error for id=" + pRecordId);
            pMessage = "JSON parse error: " + jpe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);

        } catch (java.io.IOException ioe) {
            LOG.log(Level.SEVERE, ioe, () -> LOG_PREFIX + "processOfsRequest: I/O error for id=" + pRecordId);
            pMessage = "I/O error: " + ioe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);

        } catch (RuntimeException re) {
            LOG.log(Level.SEVERE, re, () -> LOG_PREFIX + "processOfsRequest: Runtime error for id=" + pRecordId);
            pMessage = "Runtime error: " + re.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
        } finally {
            // Cleanup
            pOriginalItem = null;
            pResponseId = null;
        }
    }

    /**
     * Phase 2: Check OFS response - retrieve response from OFS.REQUEST.DETAIL and
     * publish to MQ.
     */
    private void checkOfsResponse(String pResponseId) {
        OfsRequestDetailRecord pOfsRequestDetailRecord = null;
        TBoolean pExists = null;
        String pMsgOut = null;
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        String pTransactRef = "";

        LOG.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() START ===");

        // Retrieve transaction metadata from cache
        TransactionMetadata pMetadata;
        synchronized (TRANSACTION_CACHE) {
            pMetadata = TRANSACTION_CACHE.get(pResponseId);
        }

        if (pMetadata == null) {
            LOG.log(Level.WARNING, LOG_PREFIX + "checkOfsResponse: No metadata found for responseId={0}", pResponseId);
            return;
        }

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: Checking OFS response for record ID: {0}", pResponseId);

            // Initialize TBoolean with null - same pattern as test class
            pExists = new TBoolean(null);
            pOfsRequestDetailRecord = mDataAccess.getRequestResponse(pResponseId, pExists);
            LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: OFS record exists: {0}", pExists);

            // Check if record exists and has message output
            if (pOfsRequestDetailRecord != null && pOfsRequestDetailRecord.getMsgOut() != null) {
                pMsgOut = pOfsRequestDetailRecord.getMsgOut().getValue();

                if (pMsgOut != null && !pMsgOut.isEmpty()) {
                    LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: OFS response message retrieved successfully");
                    LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: ORD OUT MSG retrieved");

                    // Check for OFS errors
                    if (pMsgOut.contains("/-1/")) {
                        pStatus = MSG_FAILURE;
                        pMessage = "T24 OFS Error: " + pMsgOut;
                        pTransactRef = "ERROR";
                        LOG.log(Level.WARNING, LOG_PREFIX + "checkOfsResponse: OFS Error detected: {0}", pMsgOut);

                    } else {
                        // Parse transaction reference from OFS response
                        pTransactRef = pOfsRequestDetailRecord.getTransReference().toString();
                        pStatus = MSG_SUCCESS;
                        pMessage = "Forex transaction processed successfully";
                        LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: Transaction successful: ref={0}",
                                pTransactRef);
                    }

                    // Publish response
                    publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef, pMetadata.originalItem);

                    // Acknowledge MQ message on success
                    if (MSG_SUCCESS.equalsIgnoreCase(pStatus) && "WMQ".equalsIgnoreCase(pMetadata.adapterMode)) {
                        CsdBloombergFxAdapter.acknowledgeMqMessage(pMetadata.originalId);
                        LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: MQ message acknowledged for id={0}",
                                pMetadata.originalId);
                    }

                    // Remove from cache after successful processing
                    synchronized (TRANSACTION_CACHE) {
                        TRANSACTION_CACHE.remove(pResponseId);
                    }

                } else {
                    LOG.log(Level.WARNING, LOG_PREFIX + "checkOfsResponse: OFS message output is null or empty");
                }

            } else {
                LOG.log(Level.WARNING,
                        LOG_PREFIX + "checkOfsResponse: OFS request detail record or message output is null");
            }

            LOG.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() COMPLETE ===");

        } catch (T24CoreException e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "checkOfsResponse: Error checking OFS response", e);
            pMessage = "Error checking OFS response: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef, pMetadata.originalItem);

            // Remove from cache on error
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.remove(pResponseId);
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "checkOfsResponse: Unexpected error in checkOfsResponse", e);
            pMessage = "Unexpected error: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef, pMetadata.originalItem);

            // Remove from cache on error
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.remove(pResponseId);
            }

        } finally {
            // Cleanup
            pOfsRequestDetailRecord = null;
            pExists = null;
            pMsgOut = null;
        }
    }

    /**
     * Retrieves transaction item from FILE or WMQ based on adapter mode. NOW
     * HANDLES SINGLE FOREX_TRANSACTION OBJECT FORMAT
     */
    private JsonNode retrieveTransactionItem(String pId) throws java.io.IOException {
        LOG.log(Level.INFO, LOG_PREFIX + "=== retrieveTransactionItem() START for ID: {0} ===", pId);

        JsonNode result = null;
        if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            result = processFileRecord(pId);
        } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            result = processWmqRecord(pId);
        } else {
            LOG.log(Level.SEVERE, LOG_PREFIX + "retrieveTransactionItem: Unknown adapter flag: {0}", mAdapterFlag);
        }

        LOG.log(Level.INFO, LOG_PREFIX + "=== retrieveTransactionItem() COMPLETE ===");
        return result;
    }

    /**
     * Processes a FILE mode record.
     */
    private JsonNode processFileRecord(String pId) throws java.io.IOException {
        FileItemRef pRef = CsdBloombergFxAdapter.parseFileItemRef(pId);
        if (pRef == null) {
            LOG.log(Level.WARNING, LOG_PREFIX + "processFileRecord: FILE mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CsdBloombergFxAdapter.readRoot(pRef.file(), OBJECT_MAPPER);
        if (!CsdBloombergFxMapper.hasForexTransacts(pRoot)) {
            LOG.log(Level.WARNING, LOG_PREFIX + "processFileRecord: FILE mode: no FOREX_TRANSACTION in {0}",
                    pRef.file().getFileName());
            return null;
        }

        return CsdBloombergFxMapper.getForexTransactAt(pRoot, pRef.index());
    }

    /**
     * Processes a WMQ mode record.
     */
    private JsonNode processWmqRecord(String pId) throws java.io.IOException {
        MqItemRef pRef = CsdBloombergFxAdapter.parseMqItemRef(pId);
        if (pRef == null) {
            LOG.log(Level.WARNING, LOG_PREFIX + "processWmqRecord: WMQ mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CsdBloombergFxAdapter.readMqMessage(pRef.messageId(), OBJECT_MAPPER);
        if (pRoot == null || !CsdBloombergFxMapper.hasForexTransacts(pRoot)) {
            LOG.log(Level.WARNING, LOG_PREFIX + "processWmqRecord: WMQ mode: no FOREX_TRANSACTION in message {0}",
                    pRef.messageId());
            return null;
        }

        return CsdBloombergFxMapper.getForexTransactAt(pRoot, pRef.index());
    }

    /**
     * Builds the responseId for T24 based on the adapter mode.
     */
    private String buildResponseId(String pId) {
        if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            MqItemRef pRef = CsdBloombergFxAdapter.parseMqItemRef(pId);
            if (pRef != null && pRef.messageId() != null) {
                String pJmsMessageId = pRef.messageId();
                LOG.log(Level.INFO, LOG_PREFIX + "buildResponseId: Using JMS Message ID as responseId: {0}",
                        pJmsMessageId);
                return pJmsMessageId;
            }
        } else if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            return extractPrefix(pId);
        }
        return extractPrefix(pId);
    }

    /**
     * Builds a ForexRecord from the mapped data and populates the transaction
     * lists.
     */
    private boolean buildForexRecord(String pResponseId, Map<String, String> pData,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {

        ForexRecord pFxRecord = null;
        SynchronousTransactionData pTxnData = null;

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== buildForexRecord() START ===");
            LOG.log(Level.INFO, LOG_PREFIX + "buildForexRecord: Building forex record for responseId: {0}",
                    pResponseId);

            // Extract fields from mapped data
            String pCounterPty = pData.getOrDefault("CPTY", "");
            String pDealType = pData.getOrDefault("DTYP", "");
            String pDealerDesk = pData.getOrDefault("DESK", "");
            String pDealDate = pData.getOrDefault("DDAT", "");
            String pCcyBought = pData.getOrDefault("CCYB", "");
            String pAmtBought = pData.getOrDefault("BAMT", "");
            String pVDateBuy = pData.getOrDefault("VBUY", "");
            String pCcySold = pData.getOrDefault("CCYS", "");
            String pAmtSold = pData.getOrDefault("SAMT", "");
            String pDateSell = pData.getOrDefault("VSEL", "");
            String pSpotRate = pData.getOrDefault("SPRT", "");
            String pSpotDate = pData.getOrDefault("SPDT", "");
            String pBaseCcy = pData.getOrDefault("BCCY", "");
            String pDealBroker = pData.getOrDefault("BRKR", "");
            String pCptCorName = pData.getOrDefault("CCNO", "");
            String pCptCorCity = pData.getOrDefault("CCAD", "");
            String pAcToCharge = pData.getOrDefault("CBNK", "");
            String pIntRateBuy = pData.getOrDefault("INTB", "");
            String pIntRateSell = pData.getOrDefault("INTS", "");

            // Extract deal-specific fields
            String pForwardRate = pData.getOrDefault("FWRT", "");
            String pLeg1FwdRate = pData.getOrDefault("LG1R", "");
            String pFwdFwdSwap = pData.getOrDefault("FFWS", "");
            String pUnevenSwap = pData.getOrDefault("UEVS", "");
            String pSwapRef1 = pData.getOrDefault("SRF1", "");
            String pSwapRef2 = pData.getOrDefault("SRF2", "");
            String pSwBaseCcy = pData.getOrDefault("SCCY", "");
            String ourAccPay = pData.getOrDefault("OACP", "");
            String ourAccRec = pData.getOrDefault("OACR", "");
            // Get OFS version from properties based on deal type
            String pOfsVersion = CONFIG.getFxOfsVersionForDealType(pDealType);
            if (pOfsVersion == null || pOfsVersion.isEmpty()) {
                pOfsVersion = mOfsVersionDefault;
            }
            LOG.log(Level.INFO, LOG_PREFIX + "buildForexRecord: Using OFS Version: {0} for responseId={1}",
                    new Object[] { pOfsVersion, pResponseId });

            // Create Forex record
            pFxRecord = new ForexRecord();
            pFxRecord.setCounterparty(pCounterPty);
            pFxRecord.setDealerDesk(pDealerDesk);
            pFxRecord.setDealDate(pDealDate);
            pFxRecord.setDealerNotes(pCounterPty, 0);
            pFxRecord.setCurrencyBought(pCcyBought);
            pFxRecord.setAmountBought(pAmtBought);
            pFxRecord.setValueDateBuy(pVDateBuy);
            pFxRecord.setCurrencySold(pCcySold);
            pFxRecord.setAmountSold(pAmtSold);
            pFxRecord.setValueDateSell(pDateSell);
            pFxRecord.setSpotRate(pSpotRate);
            pFxRecord.setSpotDate(pSpotDate);
            pFxRecord.setBaseCcy(pBaseCcy);
            pFxRecord.setBroker(pDealBroker);
            pFxRecord.setCpyCorrName(pCptCorName);
            pFxRecord.setCpyCorrCity(pCptCorCity);
            pFxRecord.setAccountToCharge(pAcToCharge);
            pFxRecord.setIntRateBuy(pIntRateBuy);
            pFxRecord.setIntRateSell(pIntRateSell);

            OurAccountPayClass AcctPay = new OurAccountPayClass();
            AcctPay.setOurAccountRec(ourAccRec);
            AcctPay.setOurAccountPay(ourAccPay);
            pFxRecord.setOurAccountPay(AcctPay, 0);

            // Set deal-specific fields based on DEAL_TYPE
            if (pDealType != null && !pDealType.trim().isEmpty()) {
                String pNormalizedDealType = pDealType.trim().toUpperCase();

                switch (pNormalizedDealType) {
                case "FW": // Forward deal
                    if (!pForwardRate.isEmpty()) {
                        LOG.log(Level.FINE, LOG_PREFIX + "buildForexRecord: FW: set FORWARD_RATE={0}", pForwardRate);
                    }
                    break;

                case "SW": // Swap deal
                    if (!pSwBaseCcy.isEmpty()) {
                        pFxRecord.setSwapBaseCcy(pSwBaseCcy);
                    }
                    if (!pLeg1FwdRate.isEmpty()) {
                        pFxRecord.setLeg1FwdRate(pLeg1FwdRate);
                    }
                    if (!pForwardRate.isEmpty()) {
                        pFxRecord.setForwardRate(pForwardRate);
                    }
                    if (!pFwdFwdSwap.isEmpty()) {
                        pFxRecord.setFwdFwdSwap(pFwdFwdSwap);
                    }
                    if (!pUnevenSwap.isEmpty()) {
                        pFxRecord.setUnevenSwap(pUnevenSwap);
                    }
                    if (!pSwapRef1.isEmpty()) {
                        pFxRecord.setSwapRefNo(pSwapRef1, 0);
                    }
                    if (!pSwapRef2.isEmpty()) {
                        pFxRecord.setSwapRefNo(pSwapRef2, 1);
                    }
                    LOG.log(Level.FINE, LOG_PREFIX + "buildForexRecord: SW: set swap-specific fields");
                    break;

                case "SP": // Spot deal - no additional fields
                    LOG.log(Level.FINE, LOG_PREFIX + "buildForexRecord: SP: no deal-specific fields to set");
                    break;

                default:
                    LOG.log(Level.WARNING, LOG_PREFIX + "buildForexRecord: Unknown DEAL_TYPE: {0}", pDealType);
                }
            }

            // Log all extracted values
            LOG.log(Level.INFO, LOG_PREFIX
                    + "buildForexRecord: Populating ForexRecord - COUNTERPARTY={0}, DEAL_TYPE={1}, DEALER_DESK={2}, "
                    + "DEAL_DATE={3}, CURRENCY_BOUGHT={4}, AMOUNT_BOUGHT={5}, VALUE_DATE_BUY={6}, CURRENCY_SOLD={7}, "
                    + "AMOUNT_SOLD={8}, VALUE_DATE_SELL={9}, SPOT_RATE={10}, SPOT_DATE={11}, BASE_CCY={12}, BROKER={13}, "
                    + "CPY_CORR_NAME={14}, CPY_CORR_CITY={15}, ACCOUNT_TO_CHARGE={16}, INT_RATE_BUY={17}, INT_RATE_SELL={18}",
                    new Object[] { pCounterPty, pDealType, pDealerDesk, pDealDate, pCcyBought, pAmtBought, pVDateBuy,
                            pCcySold, pAmtSold, pDateSell, pSpotRate, pSpotDate, pBaseCcy, pDealBroker, pCptCorName,
                            pCptCorCity, pAcToCharge, pIntRateBuy, pIntRateSell });

            // Create transaction envelope with responseId - FIXED: Removed companyId
            pTxnData = new SynchronousTransactionData();
            pTxnData.setResponseId(pResponseId);
            pTxnData.setVersionId(pOfsVersion);
            pTxnData.setFunction(mOfsFunction);
            pTxnData.setNumberOfAuthoriser("0");
            pTxnData.setSourceId(mOfsSource);
            pTxnData.setCompanyId("GB0010001");

            // Add to transaction data and records
            pTransactionData.add(pTxnData);
            pRecords.add(pFxRecord.toStructure());
            LOG.log(Level.INFO, LOG_PREFIX
                    + "buildForexRecord: Forex record populated successfully Transaction data added with responseId={0}",
                    pResponseId);
            return true;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "buildForexRecord: Error building ForexRecord", e);
            return false;
        } finally {
            // Cleanup
            pFxRecord = null;
            pTxnData = null;
        }
    }

    /**
     * Parses the transaction reference from the OFS response.
     */
    private String parseTransactionRef(String pOfsResponse) {
        try {
            if (pOfsResponse.contains("/") && pOfsResponse.contains(",")) {
                int firstSlash = pOfsResponse.indexOf("/");
                int secondSlash = pOfsResponse.indexOf("/", firstSlash + 1);
                int commaAfter = pOfsResponse.indexOf(",", secondSlash);

                if (firstSlash > 0 && secondSlash > firstSlash && commaAfter > secondSlash) {
                    String pTxnRef = pOfsResponse.substring(secondSlash + 1, commaAfter);
                    LOG.log(Level.INFO, LOG_PREFIX + "parseTransactionRef: Parsed transaction reference: {0}", pTxnRef);
                    return pTxnRef;
                }
            }
            return pOfsResponse.length() > 50 ? pOfsResponse.substring(0, 50) : pOfsResponse;
        } catch (Exception e) {
            LOG.log(Level.WARNING, LOG_PREFIX + "parseTransactionRef: Error parsing transaction reference from OFS response", e);
            return MSG_UNKNOWN;
        }
    }


    /**
     * Builds response and publishes using CsdBloombergFxPayload and
     * CsdBloombergFxProducer.
     */
    private void publishResponse(String pId, String pStatus, String pMessage, String pTransactRef,
            JsonNode pOriginalItem) {
        try {
            String pJsonResponse = mPayloadHandler.buildResponse(pStatus, pMessage, pTransactRef, pOriginalItem);
            LOG.log(Level.INFO, LOG_PREFIX + "publishResponse: Built response: {0}", pJsonResponse);
            mProducer.publishResponse(pJsonResponse, mAdapterFlag, pId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e, () -> LOG_PREFIX + "publishResponse: Error publishing response for id=" + pId);
        }
    }

    /**
     * Persists the failed transaction source to EXCEPTS directory.
     */
    private void persistToExcepts(String pId, String pReason) {
        try {
            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                FileItemRef pRef = CsdBloombergFxAdapter.parseFileItemRef(pId);
                if (pRef != null) {
                    CsdBloombergFxAdapter.persistFailedFileItem(pRef, mExceptsDir, pReason);
                } else {
                    LOG.log(Level.WARNING, LOG_PREFIX + "persistToExcepts: could not parse FILE ref from id={0}", pId);
                }
            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                MqItemRef pRef = CsdBloombergFxAdapter.parseMqItemRef(pId);
                if (pRef != null) {
                    CsdBloombergFxAdapter.persistFailedMqItem(pRef, mExceptsDir, pReason);
                } else {
                    LOG.log(Level.WARNING, LOG_PREFIX + "persistToExcepts: could not parse WMQ ref from id={0}", pId);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex, () -> LOG_PREFIX + "persistToExcepts: persistToExcepts failed for id=" + pId);
        }
    }

    /**
     * Extracts prefix from transaction ID for response identification (FILE mode
     * fallback).
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
            LOG.log(Level.WARNING, e, () -> LOG_PREFIX + "extractPrefix: Error extracting prefix from: " + pInput);
            return MSG_UNKNOWN;
        }
    }
}