package com.cbn.bloomberg.pr;

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

import com.cbn.bloomberg.pr.CbnPrAdapter.FileItemRef;
import com.cbn.bloomberg.pr.CbnPrAdapter.MqItemRef;
import com.cbn.bloomberg.util.CbnTfProperties;
import com.cbn.bloomberg.util.CbnTfBackup;
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
import com.temenos.t24.api.tables.cbnrepoplacement.CbnRepoPlacementRecord;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;


/**
 * Title: CbnPrService.java Author: CSD Development Team Date Created: 2025-10-11 Last
 * Modified: 2025-11-09
 *
 * Purpose: Bloomberg PR Service Hook supporting dual ingestion modes: FILE or WMQ (IBM MQ). The
 * adapter mode is controlled by bloomberg.properties.
 *
 * Two-Phase Processing Pattern: - Phase 1 (PROCESS.PR): Submit OFS request to T24 with responseId -
 * Phase 2 (CHECK.RESPONSE): Retrieve OFS response from OFS.REQUEST.DETAIL and publish to MQ
 *
 * This approach ensures T24 has time to process the OFS request before attempting to retrieve the
 * response.
 *
 * Modification Details: ---- 09/11/25 - Updated to handle single REPO object format
 */

public class CbnPrService extends ServiceLifecycle {

    // ==== CONSTANTS ====
    private static final Logger LOG = CbnTfLogTracer.forClass(CbnPrService.class);
    private static final CbnTfProperties CONFIG = CbnTfProperties.getInstance();

    // Control list constants
    private static final String CONTROL_PROCESS_PR = "PROCESS.PR";
    private static final String CONTROL_CHECK_RESPONSE = "CHECK.RESPONSE";

    // Message constants
    private static final String MSG_SUCCESS = "success";
    private static final String MSG_FAILURE = "failure";
    private static final String MSG_UNKNOWN = "UNKNOWN";

    // Log prefix
    private static final String LOG_PREFIX = "[CbnPrService] ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==== CONFIGURATION ====
    private final String mAdapterFlag = CONFIG.getDefAdapter();
    private final String mOfsFunction = CONFIG.getOfsFunction();
    private final String mOfsSource = CONFIG.getOfsSource();
    private final String pOfsVersion = CONFIG.getOfsVersionPr();
    private final String mFilePattern = CONFIG.getNfsFilePattern();
    private final Path mInboundDir = Paths.get(CONFIG.getNfsInboundDir());
    private final Path mProcessDir = Paths.get(CONFIG.getNfsDoneDir());
    private final Path mExceptsDir = Paths.get(CONFIG.getNfsErrorDir());

    // ==== INSTANCE VARIABLES ====
    private String mCompanyId = "BNK";
    private Session mSession = null;
    private DataAccess mDataAccess = null;
    private final CbnPrPayload mPayloadHandler = new CbnPrPayload(OBJECT_MAPPER);
    private final CbnPrProducer mProducer = new CbnPrProducer();

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
        String bloombergId;

        TransactionMetadata(String pOriginalId, JsonNode pOriginalItem, String pAdapterMode, String pBloombergId) {
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
        try {

            LOG.log(Level.INFO, LOG_PREFIX + "=== initialise() START ===");
            mSession = new Session(this);
            mDataAccess = new DataAccess(this);
            mCompanyId = mSession.getCompanyId();
            LOG.log(Level.INFO, LOG_PREFIX + "initialise: Service initialized for company: {0}",
                    mCompanyId);
            LOG.log(Level.INFO, LOG_PREFIX + "=== initialise() COMPLETE ===");
        } catch (T24CoreException e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "initialise: Error initializing service", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "initialise: Unexpected error during initialization",
                    e);
        }
    }

    /**
     * Retrieves the list of transaction IDs to be processed by the service. Implements two-phase
     * processing: - Phase 1: PROCESS.PR - Get new transactions from FILE/WMQ - Phase 2:
     * CHECK.RESPONSE - Get pending OFS responses
     */
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> pRecordIds = null;
        String pControlItem = null;

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== getIds() START ===");

            // Ensure initialization if not already done
            if (mSession == null || mDataAccess == null) {
                LOG.log(Level.WARNING,
                        LOG_PREFIX + "getIds: Service not initialized, initializing now");
                initialise(serviceData);
            }

            // Initialize control list if empty - FIXED: Match test class pattern
            if (controlList == null || controlList.isEmpty()) {
                if (controlList == null) {
                    controlList = new ArrayList<>();
                }
                controlList.add(0, CONTROL_PROCESS_PR);
                controlList.add(1, CONTROL_CHECK_RESPONSE);
            }

            pControlItem = controlList.get(0);
            LOG.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, adapterFlag={1}",
                    new Object[] { pControlItem, mAdapterFlag });

            // Process based on control item
            switch (pControlItem) {
                case CONTROL_PROCESS_PR:
                    pRecordIds = getNewTransactionIds();
                    break;

                case CONTROL_CHECK_RESPONSE:
                    pRecordIds = getPendingResponseIds();
                    break;

                default:
                    LOG.log(Level.WARNING, LOG_PREFIX
                            + "getIds: Unrecognized controlList option: {0}. Returning empty list.",
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
     * Gets new transaction IDs from FILE or WMQ sources. NOW HANDLES SINGLE REPO OBJECT FORMAT
     */
    private List<String> getNewTransactionIds() {
        List<String> pIds = new ArrayList<>();

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== getNewTransactionIds() START ===");

            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                LOG.log(Level.INFO, LOG_PREFIX + "getNewTransactionIds: FILE mode: scanning {0}",
                        mInboundDir);

                if (!Files.isDirectory(mInboundDir)) {
                    LOG.log(Level.WARNING,
                            LOG_PREFIX + "getNewTransactionIds: Inbound directory not found: {0}",
                            mInboundDir);
                    return pIds;
                }

                pIds.addAll(CbnPrAdapter.scanDirectoryIds(mInboundDir, mFilePattern,
                        mProcessDir, OBJECT_MAPPER));

            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                LOG.log(Level.INFO,
                        LOG_PREFIX + "getNewTransactionIds: WMQ mode: consuming messages from MQ");
                pIds.addAll(CbnPrAdapter.extractIdsFromWmq(OBJECT_MAPPER));

            } else {
                LOG.log(Level.SEVERE,
                        LOG_PREFIX + "getNewTransactionIds: Unknown adapter flag: {0}",
                        mAdapterFlag);
            }

            LOG.log(Level.INFO,
                    LOG_PREFIX + "=== getNewTransactionIds() COMPLETE - Found {0} IDs ===",
                    pIds.size());

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
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
            LOG.log(Level.INFO, LOG_PREFIX + "=== getPendingResponseIds() START ===");

            synchronized (TRANSACTION_CACHE) {
                // Get all responseIds from cache
                pResponseIds.addAll(TRANSACTION_CACHE.keySet());
            }

            LOG.log(Level.INFO, LOG_PREFIX + "getPendingResponseIds: Found {0} pending responses",
                    pResponseIds.size());

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
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

        LOG.log(Level.INFO, LOG_PREFIX + "=== updateRecord() START ===");
        LOG.log(Level.INFO,
                LOG_PREFIX + "updateRecord: Processing updateRecord for controlItem: {0}, ID: {1}",
                new Object[] { controlItem, id });

        try {
            // Ensure initialization if not already done
            if (mSession == null || mDataAccess == null) {
                LOG.log(Level.WARNING,
                        LOG_PREFIX + "updateRecord: Service not initialized, initializing now");
                initialise(serviceData);
            }

            // FIXED: Added null/empty check for controlItem
            if (controlItem == null || controlItem.isEmpty()) {
                LOG.log(Level.WARNING, LOG_PREFIX
                        + "updateRecord: Control item is null or empty, defaulting to PROCESS.PR");
                controlItem = CONTROL_PROCESS_PR;
            }

            // Process based on control item
            switch (controlItem) {
                case CONTROL_PROCESS_PR:
                    processOfsRequest(id, transactionData, records);
                    break;

                case CONTROL_CHECK_RESPONSE:
                    checkOfsResponse(id);
                    break;

                default:
                    LOG.log(Level.WARNING,
                            LOG_PREFIX + "updateRecord: Unrecognized control item: {0}",
                            controlItem);
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
     * Phase 1: Process OFS request - retrieve transaction, map to RO record, submit to T24. NOW
     * HANDLES SINGLE REPO OBJECT FORMAT
     */
    private void processOfsRequest(String pRecordId,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        JsonNode pOriginalItem = null;
        String pResponseId = null;
        String bloombergId = null;

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() START ===");
            LOG.log(Level.INFO,
                    LOG_PREFIX + "processOfsRequest: Processing OFS request for record ID: {0}",
                    pRecordId);

            // Step 1: Retrieve the item based on adapter mode
            JsonNode pItem = retrieveTransactionItem(pRecordId);
            
            CbnTfBackup.backupMessage(pItem.toString(), "REPO", pRecordId);
            bloombergId = pItem.path("BLOOMBERG_ID").asText("");
            if (pItem == null) {
                pMessage = "Item not found or invalid REPO";
                LOG.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", null, bloombergId);
                return;
            }

            pOriginalItem = pItem;
            LOG.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Retrieved item for id={0}",
                    pRecordId);

            // Step 2: Map JSON to RO field map
            Map<String, String> pData = CbnPrMapper.mapRepositoryToRo(pItem);
            if (pData == null || pData.isEmpty()) {
                pMessage = "Mapping returned no data";
                LOG.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem, bloombergId);
                return;
            }

            LOG.log(Level.FINE, LOG_PREFIX + "processOfsRequest: Mapped data for id={0}",
                    pRecordId);

            // Step 3: Build responseId
            pResponseId = buildResponseId(pRecordId);
            LOG.log(Level.INFO, LOG_PREFIX + "processOfsRequest: Built responseId: {0}",
                    pResponseId);

            // Step 4: Build and populate RO record
            boolean pSuccess = buildPrRecord(pResponseId, pData, pTransactionData, pRecords);
            if (!pSuccess) {
                pMessage = "Validation failed while building FundsTransferRecord";
                LOG.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);
                return;
            }

            LOG.log(Level.INFO, LOG_PREFIX
                    + "processOfsRequest: Successfully prepared OFS transaction for RO update");

            // Step 5: Store transaction metadata in cache for Phase 2
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.put(pResponseId,
                        new TransactionMetadata(pRecordId, pOriginalItem, mAdapterFlag,bloombergId));
            }

            LOG.log(Level.INFO,
                    LOG_PREFIX
                            + "processOfsRequest: Transaction metadata cached for responseId={0}",
                    pResponseId);
            LOG.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() COMPLETE ===");

        } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
            LOG.log(Level.SEVERE, jpe,
                    () -> LOG_PREFIX + "processOfsRequest: JSON parse error for id=" + pRecordId);
            pMessage = "JSON parse error: " + jpe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);

        } catch (java.io.IOException ioe) {
            LOG.log(Level.SEVERE, ioe,
                    () -> LOG_PREFIX + "processOfsRequest: I/O error for id=" + pRecordId);
            pMessage = "I/O error: " + ioe.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);

        } catch (RuntimeException re) {
            LOG.log(Level.SEVERE, re,
                    () -> LOG_PREFIX + "processOfsRequest: Runtime error for id=" + pRecordId);
            pMessage = "Runtime error: " + re.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem,bloombergId);
        } finally {
            // Cleanup
            pOriginalItem = null;
            pResponseId = null;
        }
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

        LOG.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() START ===");

        // Retrieve transaction metadata from cache
        TransactionMetadata pMetadata;
        synchronized (TRANSACTION_CACHE) {
            pMetadata = TRANSACTION_CACHE.get(pResponseId);
        }

        if (pMetadata == null) {
            LOG.log(Level.WARNING,
                    LOG_PREFIX + "checkOfsResponse: No metadata found for responseId={0}",
                    pResponseId);
            return;
        }

        try {
            LOG.log(Level.INFO,
                    LOG_PREFIX + "checkOfsResponse: Checking OFS response for record ID: {0}",
                    pResponseId);

            // Initialize TBoolean with null - same pattern as test class
            pExists = new TBoolean(null);
            pOfsRequestDetailRecord = mDataAccess.getRequestResponse(pResponseId, pExists);
            LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: OFS record exists: {0}", pExists);

            // Check if record exists and has message output
            if (pOfsRequestDetailRecord != null && pOfsRequestDetailRecord.getMsgOut() != null) {
                pMsgOut = pOfsRequestDetailRecord.getMsgOut().getValue();

                if (pMsgOut != null && !pMsgOut.isEmpty()) {
                    LOG.log(Level.INFO, LOG_PREFIX
                            + "checkOfsResponse: OFS response message retrieved successfully");
                    LOG.log(Level.INFO, LOG_PREFIX + "checkOfsResponse: ORD OUT MSG retrieved");

                    // Check for OFS errors
                    if (pMsgOut.contains("/-1/")) {
                        pStatus = MSG_FAILURE;
                        pMessage = "T24 OFS Error: " + pMsgOut;
                        pTransactRef = "ERROR";
                        LOG.log(Level.WARNING,
                                LOG_PREFIX + "checkOfsResponse: OFS Error detected: {0}", pMsgOut);

                    } else {
                        // Parse transaction reference from OFS response
                        pTransactRef = pOfsRequestDetailRecord.getTransReference().toString();
                        pStatus = MSG_SUCCESS;
                        pMessage = "FundsTransfer transaction processed successfully";
                        LOG.log(Level.INFO,
                                LOG_PREFIX + "checkOfsResponse: Transaction successful: ref={0}",
                                pTransactRef);
                    }

                    // Publish response
                    publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                            pMetadata.originalItem, pMetadata.bloombergId);

                    // Acknowledge MQ message on success
                    if (MSG_SUCCESS.equalsIgnoreCase(pStatus)
                            && "WMQ".equalsIgnoreCase(pMetadata.adapterMode)) {
                        CbnPrAdapter.acknowledgeMqMessage(pMetadata.originalId);
                        LOG.log(Level.INFO,
                                LOG_PREFIX + "checkOfsResponse: MQ message acknowledged for id={0}",
                                pMetadata.originalId);
                    }

                    // Remove from cache after successful processing
                    synchronized (TRANSACTION_CACHE) {
                        TRANSACTION_CACHE.remove(pResponseId);
                    }

                } else {
                    LOG.log(Level.WARNING,
                            LOG_PREFIX + "checkOfsResponse: OFS message output is null or empty");
                }

            } else {
                LOG.log(Level.WARNING, LOG_PREFIX
                        + "checkOfsResponse: OFS request detail record or message output is null");
            }

            LOG.log(Level.INFO, LOG_PREFIX + "=== checkOfsResponse() COMPLETE ===");

        } catch (T24CoreException e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "checkOfsResponse: Error checking OFS response", e);
            pMessage = "Error checking OFS response: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                    pMetadata.originalItem,pMetadata.bloombergId);

            // Remove from cache on error
            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.remove(pResponseId);
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    LOG_PREFIX + "checkOfsResponse: Unexpected error in checkOfsResponse", e);
            pMessage = "Unexpected error: " + e.getMessage();
            publishResponse(pMetadata.originalId, pStatus, pMessage, pTransactRef,
                    pMetadata.originalItem,pMetadata.bloombergId);

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
     * Retrieves transaction item from FILE or WMQ based on adapter mode. NOW HANDLES SINGLE REPO
     * OBJECT FORMAT
     */
    private JsonNode retrieveTransactionItem(String pId) throws java.io.IOException {
        LOG.log(Level.INFO, LOG_PREFIX + "=== retrieveTransactionItem() START for ID: {0} ===",
                pId);

        JsonNode result = null;
        if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            result = processFileRecord(pId);
        } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            result = processWmqRecord(pId);
        } else {
            LOG.log(Level.SEVERE, LOG_PREFIX + "retrieveTransactionItem: Unknown adapter flag: {0}",
                    mAdapterFlag);
        }

        LOG.log(Level.INFO, LOG_PREFIX + "=== retrieveTransactionItem() COMPLETE ===");
        return result;
    }

    /**
     * Processes a FILE mode record.
     */
    private JsonNode processFileRecord(String pId) throws java.io.IOException {
        FileItemRef pRef = CbnPrAdapter.parseFileItemRef(pId);
        if (pRef == null) {
            LOG.log(Level.WARNING,
                    LOG_PREFIX + "processFileRecord: FILE mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CbnPrAdapter.readRoot(pRef.file(), OBJECT_MAPPER);
        if (!CbnPrMapper.hasRepository(pRoot)) {
            LOG.log(Level.WARNING, LOG_PREFIX + "processFileRecord: FILE mode: no REPO in {0}",
                    pRef.file().getFileName());
            return null;
        }

        return CbnPrMapper.getRepositoryAt(pRoot, pRef.index());
    }

    /**
     * Processes a WMQ mode record.
     */
    private JsonNode processWmqRecord(String pId) throws java.io.IOException {
        MqItemRef pRef = CbnPrAdapter.parseMqItemRef(pId);
        if (pRef == null) {
            LOG.log(Level.WARNING,
                    LOG_PREFIX + "processWmqRecord: WMQ mode: unexpected id format: {0}", pId);
            return null;
        }

        JsonNode pRoot = CbnPrAdapter.readMqMessage(pRef.messageId(), OBJECT_MAPPER);
        if (pRoot == null || !CbnPrMapper.hasRepository(pRoot)) {
            LOG.log(Level.WARNING,
                    LOG_PREFIX + "processWmqRecord: WMQ mode: no REPO in message {0}",
                    pRef.messageId());
            return null;
        }

        return CbnPrMapper.getRepositoryAt(pRoot, pRef.index());
    }

    /**
     * Builds the responseId for T24 based on the adapter mode.
     */
    private String buildResponseId(String pId) {
        if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
            MqItemRef pRef = CbnPrAdapter.parseMqItemRef(pId);
            if (pRef != null && pRef.messageId() != null) {
                String pJmsMessageId = pRef.messageId();
                LOG.log(Level.INFO,
                        LOG_PREFIX + "buildResponseId: Using JMS Message ID as responseId: {0}",
                        pJmsMessageId);
                return pJmsMessageId;
            }
        } else if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
            return extractPrefix(pId);
        }
        return extractPrefix(pId);
    }

    /**
     * Builds a FundsTransferRecord from the mapped data and populates the transaction lists.
     */
    private boolean buildPrRecord(String pResponseId, Map<String, String> pData,
            List<SynchronousTransactionData> pTransactionData, List<TStructure> pRecords) {

        CbnRepoPlacementRecord pRepoRecord = null;
        SynchronousTransactionData pTxnData = null;

        try {
            LOG.log(Level.INFO, LOG_PREFIX + "=== buildPrRecord() START ===");
            LOG.log(Level.INFO,
                    LOG_PREFIX + "buildPrRecord: Building fundstransfer record for responseId: {0}",
                    pResponseId);

            // Extract fields
            String counterParty = pData.getOrDefault("CPTY", "");
            String repoCurrency = pData.getOrDefault("DCCY", "");
            String tradeDate = pData.getOrDefault("TDTE", "");
            String valueDate = pData.getOrDefault("VDTE", "");
            String maturityDate = pData.getOrDefault("MDTE", "");
            String principalAmt1 = pData.getOrDefault("AMT1", "");
            String principalAmt2 = pData.getOrDefault("AMT2", "");
            String repoRate = pData.getOrDefault("RRTE", "");
            String repoProduct = pData.getOrDefault("RPRD", "");
            String drawDownAcct = pData.getOrDefault("DDAC", "");
            String drawInAcct = pData.getOrDefault("DIAC", "");
            String interestRate = pData.getOrDefault("TINT", "");

            // Validate required fields
            if (drawDownAcct.isEmpty() || repoCurrency.isEmpty() || valueDate.isEmpty()) {
                LOG.log(Level.SEVERE, "[CbnPrService] Missing required fields");
                return false;
            }
            // Log extracted field values
            LOG.log(Level.INFO,
                    "[CbnPrService] Extracted fields - CPTY={0}, DCCY={1}, TDTE={2}, VDTE={3}, MDTE={4}, AMT1={5}, AMT2={6}, RRTE={7}, RPRD={8}, DDAC={9}, DIAC={10}, TINT={11}",
                    new Object[] { counterParty, repoCurrency, tradeDate, valueDate, maturityDate,
                            principalAmt1, principalAmt2, repoRate, repoProduct, drawDownAcct,
                            drawInAcct, interestRate });

            // Create FundsTransfer record
            pRepoRecord = new CbnRepoPlacementRecord();
            pRepoRecord.setCounterparty(counterParty);
            pRepoRecord.setCurrency(repoCurrency);
            pRepoRecord.setTradeDate(tradeDate);
            pRepoRecord.setValueDate(valueDate);
            pRepoRecord.setMaturityDate(maturityDate);
            pRepoRecord.setPrincipalAmount1(principalAmt1);
            pRepoRecord.setPrincipalAmount1(principalAmt2);
            pRepoRecord.setRepoRate(repoRate);
            pRepoRecord.setProduct(repoProduct);
            pRepoRecord.setDrawdownAccount(drawDownAcct);
            pRepoRecord.setDrawinAccount(drawInAcct);
            pRepoRecord.setTotInterestAmt(interestRate);

            LOG.log(Level.INFO, "[CbnPrService] Adding record: {0}", pRepoRecord);

            // Create transaction envelope with responseId - FIXED: Removed companyId
            pTxnData = new SynchronousTransactionData();
            pTxnData.setResponseId(pResponseId);
            pTxnData.setVersionId(pOfsVersion);
            pTxnData.setFunction(mOfsFunction);
            pTxnData.setNumberOfAuthoriser("0");
            pTxnData.setSourceId(mOfsSource);
            pTxnData.setCompanyId(mCompanyId);

            // Add to transaction data and records
            pTransactionData.add(pTxnData);
            pRecords.add(pRepoRecord.toStructure());
            LOG.log(Level.INFO, LOG_PREFIX
                    + "buildPrRecord: FundsTransfer record populated successfully Transaction data added with responseId={0}",
                    pResponseId);
            return true;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, LOG_PREFIX + "buildPrRecord: Error building FundsTransferRecord",
                    e);
            return false;
        } finally {
            // Cleanup
            pRepoRecord = null;
            pTxnData = null;
        }
    }

    /**
     * Builds response and publishes using CsdBloombergFtPayload and CsdBloombergFtProducer.
     */
    private void publishResponse(String pId, String pStatus, String pMessage, String pTransactRef,
            JsonNode pOriginalItem, String bloombergId ) {
        try {
            String pJsonResponse = mPayloadHandler.buildResponse(pStatus, pMessage, pTransactRef,
                    pOriginalItem,bloombergId);
            LOG.log(Level.INFO, LOG_PREFIX + "publishResponse: Built response: {0}", pJsonResponse);
            mProducer.publishResponse(pJsonResponse, mAdapterFlag, pId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e,
                    () -> LOG_PREFIX + "publishResponse: Error publishing response for id=" + pId);
        }
    }

    /**
     * Persists the failed transaction source to EXCEPTS directory.
     */
    private void persistToExcepts(String pId, String pReason) {
        try {
            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                FileItemRef pRef = CbnPrAdapter.parseFileItemRef(pId);
                if (pRef != null) {
                    CbnPrAdapter.persistFailedFileItem(pRef, mExceptsDir, pReason);
                } else {
                    LOG.log(Level.WARNING,
                            LOG_PREFIX + "persistToExcepts: could not parse FILE ref from id={0}",
                            pId);
                }
            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                MqItemRef pRef = CbnPrAdapter.parseMqItemRef(pId);
                if (pRef != null) {
                    CbnPrAdapter.persistFailedMqItem(pRef, mExceptsDir, pReason);
                } else {
                    LOG.log(Level.WARNING,
                            LOG_PREFIX + "persistToExcepts: could not parse WMQ ref from id={0}",
                            pId);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex,
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
            LOG.log(Level.WARNING, e,
                    () -> LOG_PREFIX + "extractPrefix: Error extracting prefix from: " + pInput);
            return MSG_UNKNOWN;
        }
    }
}