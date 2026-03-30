package com.cbn.bloomberg.sc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
import com.cbn.bloomberg.util.CbnTfBackup;
import com.cbn.bloomberg.util.CbnTfLogTracer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temenos.api.TBoolean;
import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.cbninstrumentdetailint.CbnInstrumentDetailIntRecord;
import com.temenos.t24.api.tables.cbnbondtradeint.CbnBondTradeIntRecord;

/**
 * =============================================================================
 * CSD API Title: CbnScService.java
 * Author: CSD Development Team
 * Created: 2026-01-07
 * Last Modified: 2026-03-15
 * =============================================================================
 *
 * PURPOSE: Bloomberg SC Service Hook supporting FILE or WMQ ingestion.
 * Handles SECURITY_MASTER → EB.CBN.INSTRUMENT.DETAIL.INT
 *         SEC_TRADE     → EB.CBN.BOND.TRADE.INT (with conditional version)
 *
 * TWO-PHASE PATTERN:
 *   PROCESS.SC    → submit OFS request
 *   CHECK.RESPONSE → read response from OFS.REQUEST.DETAIL and publish
 * =============================================================================
 */
public class CbnScService extends ServiceLifecycle {

    private static final Logger yLOGGER = CbnTfLogTracer.forClass(CbnScService.class);
    private static final CbnTfProperties CONFIG = CbnTfProperties.getInstance();

    private static final String CONTROL_PROCESS_SC = "PROCESS.SC";
    private static final String CONTROL_CHECK_RESPONSE = "CHECK.RESPONSE";

    private static final String MSG_TYPE_SC = "SC";  // SECURITY_MASTER
    private static final String MSG_TYPE_ST = "ST";  // SEC_TRADE

    private static final String MSG_SUCCESS = "success";
    private static final String MSG_FAILURE = "failure";
    private static final String MSG_UNKNOWN = "UNKNOWN";

    private static final String LOG_PREFIX = "[CbnScService] ";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Configuration
    private final String mAdapterFlag = CONFIG.getDefAdapter();
    private final String mOfsSource   = CONFIG.getOfsSource();
    private final String mOfsFunction = CONFIG.getOfsFunction();

    private final Path mInboundDir = Paths.get(CONFIG.getNfsInboundDir());
    private final Path mProcessDir = Paths.get(CONFIG.getNfsDoneDir());
    private final Path mExceptsDir = Paths.get(CONFIG.getNfsErrorDir());
    private final String mFilePattern = CONFIG.getNfsFilePattern();

    // Instance state
    private String mCompanyId = "BNK";
    private Session mSession = null;
    private DataAccess mDataAccess = null;
    private final CbnScPayload mPayloadHandler = new CbnScPayload(OBJECT_MAPPER);
    private final CbnScProducer mProducer = new CbnScProducer();

    // Cache for cross-phase metadata
    private static final Map<String, TransactionMetadata> TRANSACTION_CACHE = new HashMap<>();

    private static class TransactionMetadata {
        String originalId;
        JsonNode originalItem;
        String adapterMode;
        String messageType; // "SC" or "ST"
        String bloombergId;

        TransactionMetadata(String originalId, JsonNode originalItem, String adapterMode,
                            String messageType, String bloombergId) {
            this.originalId   = originalId;
            this.originalItem = originalItem;
            this.adapterMode  = adapterMode;
            this.messageType  = messageType;
            this.bloombergId  = bloombergId;
        }
    }

    @Override
    public void initialise(ServiceData serviceData) {
        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== initialise() START ===");
            mSession = new Session(this);
            mDataAccess = new DataAccess(this);
            mCompanyId = mSession.getCompanyId();
            yLOGGER.log(Level.INFO, LOG_PREFIX + "initialise: Service initialized for company: {0}", mCompanyId);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== initialise() COMPLETE ===");
        } catch (T24CoreException e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "initialise: Error initializing service", e);
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "initialise: Unexpected error during initialization", e);
        }
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> pRecordIds = null;
        String pControlItem = null;
        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getIds() START ===");
            if (mSession == null || mDataAccess == null) {
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "getIds: Service not initialized, initializing now");
                initialise(serviceData);
            }
            if (controlList == null || controlList.isEmpty()) {
                if (controlList == null) controlList = new ArrayList<>();
                controlList.add(0, CONTROL_PROCESS_SC);
                controlList.add(1, CONTROL_CHECK_RESPONSE);
            }
            pControlItem = controlList.get(0);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, adapterFlag={1}",
                    new Object[]{pControlItem, mAdapterFlag});

            switch (pControlItem) {
                case CONTROL_PROCESS_SC:
                    pRecordIds = getNewTransactionIds();
                    break;
                case CONTROL_CHECK_RESPONSE:
                    pRecordIds = getPendingResponseIds();
                    break;
                default:
                    yLOGGER.log(Level.WARNING, LOG_PREFIX + "getIds: Unrecognized controlList option: {0}", pControlItem);
                    pRecordIds = Collections.emptyList();
                    break;
            }
            yLOGGER.log(Level.INFO, LOG_PREFIX + "getIds: phase={0}, total IDs={1}",
                    new Object[]{pControlItem, pRecordIds.size()});
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "getIds: Error retrieving record IDs", e);
            pRecordIds = Collections.emptyList();
        }
        return pRecordIds;
    }

    private List<String> getNewTransactionIds() {
        List<String> pIds = new ArrayList<>();
        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getNewTransactionIds() START ===");
            if ("FILE".equalsIgnoreCase(mAdapterFlag)) {
                yLOGGER.log(Level.INFO, LOG_PREFIX + "FILE mode: scanning {0}", mInboundDir);
                if (!Files.isDirectory(mInboundDir)) {
                    yLOGGER.log(Level.WARNING, LOG_PREFIX + "Inbound directory not found: {0}", mInboundDir);
                    return pIds;
                }
                pIds.addAll(CbnScAdapter.scanDirectoryIds(mInboundDir, mFilePattern, mProcessDir, OBJECT_MAPPER));
            } else if ("WMQ".equalsIgnoreCase(mAdapterFlag)) {
                yLOGGER.log(Level.INFO, LOG_PREFIX + "WMQ mode: consuming messages from MQ");
                pIds.addAll(CbnScAdapter.extractIdsFromWmq(OBJECT_MAPPER));
            } else {
                yLOGGER.log(Level.SEVERE, LOG_PREFIX + "Unknown adapter flag: {0}", mAdapterFlag);
            }
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getNewTransactionIds() COMPLETE - Found {0} IDs ===", pIds.size());
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "getNewTransactionIds: Error retrieving new transaction IDs", e);
        }
        return pIds;
    }

    private List<String> getPendingResponseIds() {
        List<String> pResponseIds = new ArrayList<>();
        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== getPendingResponseIds() START ===");
            synchronized (TRANSACTION_CACHE) {
                pResponseIds.addAll(TRANSACTION_CACHE.keySet());
            }
            yLOGGER.log(Level.INFO, LOG_PREFIX + "getPendingResponseIds: Found {0} pending responses", pResponseIds.size());
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "getPendingResponseIds: Error retrieving pending response IDs", e);
        }
        return pResponseIds;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
                             TransactionControl transactionControl,
                             List<SynchronousTransactionData> transactionData,
                             List<TStructure> records) {
        yLOGGER.log(Level.INFO, LOG_PREFIX + "=== updateRecord() START ===");
        yLOGGER.log(Level.INFO,
                LOG_PREFIX + "updateRecord: Processing updateRecord for controlItem: {0}, ID: {1}",
                new Object[] { controlItem, id });

        try {
            if (mSession == null || mDataAccess == null) {
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "updateRecord: Service not initialized, initializing now");
                initialise(serviceData);
            }

            if (controlItem == null || controlItem.isEmpty()) {
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
                    yLOGGER.log(Level.WARNING, LOG_PREFIX + "updateRecord: Unrecognized control item: {0}", controlItem);
                    break;
            }
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "updateRecord: Error processing record", e);
        } finally {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== updateRecord() COMPLETE ===");
        }
    }

    private void processOfsRequest(String pRecordId,
                                   List<SynchronousTransactionData> pTransactionData,
                                   List<TStructure> pRecords) {
        String pStatus = MSG_FAILURE;
        String pMessage = "Unknown error";
        JsonNode pOriginalItem = null;
        String pResponseId = null;
        String pMessageType = MSG_TYPE_SC;

        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "=== processOfsRequest() START ===");

            pMessageType = detectMessageTypeFromId(pRecordId);
            yLOGGER.log(Level.INFO, LOG_PREFIX + "Detected message type: {0}", pMessageType);

            JsonNode pItem = retrieveTransactionItem(pRecordId, pMessageType);
            if (pItem == null) {
                pMessage = "Item not found or invalid " +
                        (MSG_TYPE_ST.equals(pMessageType) ? "SEC_TRADE" : "SECURITY_MASTER");
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "{0} for id={1}",new Object[] { pMessage, pRecordId } );
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", null);
                return;
            }
            pOriginalItem = pItem;

            Map<String, String> pData;
            if (MSG_TYPE_ST.equals(pMessageType)) {
                pData = CbnScMapper.mapSecTradeToBondTradeInt(pItem);
            } else {
                pData = CbnScMapper.mapSecurityMasterToInstrumentDetail(pItem);
            }

            if (pData == null || pData.isEmpty()) {
                pMessage = "Mapping returned no data";
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "{0} for id={1}", new Object[] { pMessage, pRecordId});
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
                return;
            }
            yLOGGER.log(Level.FINE, "Mapped fields for SECURITY_MASTER: {0}", pData);
            String bloombergId = pData.getOrDefault("BLOOMBERG_ID", "");
            try {
                
                CbnTfBackup.backupMessage(pOriginalItem.toString(), "SECURITY_MASTER", pMessageType);
                yLOGGER.log(Level.INFO, LOG_PREFIX + "Message backed up for BLOOMBERG_ID: {0}", bloombergId);
            } catch (Exception e) {
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "Failed to backup message: {0}", e.getMessage());
            }

            pResponseId = buildResponseId(pRecordId);

            boolean pSuccess;
            if (MSG_TYPE_ST.equals(pMessageType)) {
                pSuccess = buildBondTradeRecord(pResponseId, pData, pTransactionData, pRecords, pOriginalItem);
            } else {
                pSuccess = buildInstrumentDetailRecord(pResponseId, pData, pTransactionData, pRecords);
            }

            if (!pSuccess) {
                pMessage = "Validation failed while building record";
                yLOGGER.log(Level.WARNING, LOG_PREFIX + "processOfsRequest: {0} for id={1}",
                        new Object[] { pMessage, pRecordId });
                persistToExcepts(pRecordId, pMessage);
                publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
                return;
            }

            synchronized (TRANSACTION_CACHE) {
                TRANSACTION_CACHE.put(pResponseId, new TransactionMetadata(
                        pRecordId, pOriginalItem, mAdapterFlag, pMessageType, bloombergId));
            }

            yLOGGER.log(Level.INFO, LOG_PREFIX + "Transaction metadata cached for responseId={0}", pResponseId);
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "processOfsRequest: Error processing id={0} full eroor is ----> {1}",
                    new Object[] { pRecordId, e});
            pMessage = "Processing error: " + e.getMessage();
            persistToExcepts(pRecordId, pMessage);
            publishResponse(pRecordId, pStatus, pMessage, "", pOriginalItem);
        }
    }

    
    
    
    private boolean buildInstrumentDetailRecord(String pResponseId, Map<String, String> pData,
                                                List<SynchronousTransactionData> pTransactionData,
                                                List<TStructure> pRecords) {
        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "Building EB.CBN.INSTRUMENT.DETAIL.INT for {0}", pResponseId);

            CbnInstrumentDetailIntRecord record = new CbnInstrumentDetailIntRecord();
     
            record.setDescription(pData.getOrDefault("SDES", ""));
            record.setShortName(pData.getOrDefault("SNME", ""));
            record.setMnemonic(pData.getOrDefault("MMNE", ""));
            record.setSecurityDomicile(pData.getOrDefault("SDOM", ""));
            record.setSecurityCurrency(pData.getOrDefault("SCCY", ""));
            record.setBondOrShare(pData.getOrDefault("BOSH", ""));
            record.setPriceCurrency(pData.getOrDefault("PCCY", ""));
            record.setPriceType(pData.getOrDefault("PTYP", ""));
            record.setLastPrice(pData.getOrDefault("LPRC", ""));
            record.setInterestRate(pData.getOrDefault("IRTE", ""));
            record.setIssueDate(pData.getOrDefault("IDTE", ""));
            record.setMaturityDate(pData.getOrDefault("MDTE", ""));
            record.setNoOfPayment(pData.getOrDefault("NPAY", ""));
            record.setAccrualStartDate(pData.getOrDefault("ADTE", ""));
            record.setIntPaymentDate(pData.getOrDefault("PDTE", ""));
            record.setFirstCpnDate(pData.getOrDefault("CDTE", ""));
            record.setIsin(pData.getOrDefault("ISIN", ""));
            record.setSetupDate(pData.getOrDefault("SDTE", ""));
            record.setNdicIndicator(pData.getOrDefault("NDIC", ""));

            TStructure structure = record.toStructure();
            
            yLOGGER.log(Level.INFO, LOG_PREFIX + "Record before sending to T24:\n{0}", structure.toString());
            
            
            SynchronousTransactionData txnData = new SynchronousTransactionData();
            txnData.setResponseId(pResponseId);
            txnData.setVersionId(CONFIG.getOfsVersionScMaster());
            txnData.setFunction(mOfsFunction);
            //txnData.setNumberOfAuthoriser("0");
            txnData.setSourceId(mOfsSource);
            txnData.setCompanyId(mCompanyId);

            pTransactionData.add(txnData);
            pRecords.add(record.toStructure());

            yLOGGER.log(Level.INFO, LOG_PREFIX + "EB.CBN.INSTRUMENT.DETAIL.INT prepared successfully");
            return true;
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "Error building instrument detail record", e);
            return false;
        }
    }

    private boolean buildBondTradeRecord(String pResponseId, Map<String, String> pData,
                                         List<SynchronousTransactionData> pTransactionData,
                                         List<TStructure> pRecords,
                                         JsonNode originalItem) {
        try {
            yLOGGER.log(Level.INFO, LOG_PREFIX + "Building EB.CBN.BOND.TRADE.INT for {0}", pResponseId);

            CbnBondTradeIntRecord record = new CbnBondTradeIntRecord();

            // Main fields
            record.setSecurityNo(pData.getOrDefault("SENO", ""));
            record.setDepository(pData.getOrDefault("DEPO", ""));
            record.setTradeDate(pData.getOrDefault("TDDT", ""));
            record.setTradeCcy(pData.getOrDefault("TCCY", ""));
            record.setInterestRate(pData.getOrDefault("IRTE", ""));
            record.setInterestDays(pData.getOrDefault("IDYS", ""));
            record.setIssueDate(pData.getOrDefault("ISDT", ""));
            record.setMaturityDate(pData.getOrDefault("MTDT", ""));
            //record.setStockExchange(pData.getOrDefault("SEXC", ""));
            //record.setBloombergId(pData.getOrDefault("BLOOMBERG_ID", ""));

            // Customer level (multi-value set 0)
            record.setCustomerNo(pData.getOrDefault("CUNO", ""));
            record.setPortfolioNo(pData.getOrDefault("PFNO", ""));
            record.setCuAccountNo(pData.getOrDefault("CUAC", ""));
            record.setIntAcctNo(pData.getOrDefault("INAC", ""));
            record.setPremDiscAcct(pData.getOrDefault("PDAC", ""));
            record.setGrossAmt(pData.getOrDefault("GAMT", ""));
            record.setBrokerNo(pData.getOrDefault("BRNO", ""));
            record.setDepoBrAccountNo(pData.getOrDefault("DBAC", ""));
            record.setDescription(pData.getOrDefault("DESC", ""));
            record.setTransType(pData.getOrDefault("TTYP", ""));
            record.setPrimSecMkt(pData.getOrDefault("PMKT", ""));
            record.setValueDate(pData.getOrDefault("VLDT", ""));
            // Nominal / Price (multi-value set 0)
            record.setNominal(pData.getOrDefault("NOML", ""));
            record.setPrice(pData.getOrDefault("PRCE", ""));

            // NDIC fields
            record.setNdicEuroInvstmentCashacct(pData.getOrDefault("NDICINVCA", ""));
            record.setNdicEuroLiabCashacct(pData.getOrDefault("NDICLIBCA", ""));
            //record.setNdicIndicator(pData.getOrDefault("NDIC", ""), 0);

            // Decide version conditionally
            String version;
            if (originalItem != null && CbnScMapper.shouldUseNdicTradeVersion(originalItem)) {
                version = CONFIG.getOfsVersionScNdicTrade();
                yLOGGER.info(LOG_PREFIX + "SEC_TRADE → using NDIC.TRADDE version");
            } else {
                version = CONFIG.getOfsVersionScCbnTrade();
                yLOGGER.info(LOG_PREFIX + "SEC_TRADE → using CBN.TRADDE version");
            }

            SynchronousTransactionData txnData = new SynchronousTransactionData();
            txnData.setResponseId(pResponseId);
            txnData.setVersionId(version);
            txnData.setFunction(mOfsFunction);
            //txnData.setNumberOfAuthoriser("0");
            txnData.setSourceId(mOfsSource);
            txnData.setCompanyId(mCompanyId);

            pTransactionData.add(txnData);
            pRecords.add(record.toStructure());

            yLOGGER.log(Level.INFO, LOG_PREFIX + "EB.CBN.BOND.TRADE.INT prepared with version: {0}", version);
            return true;
        } catch (Exception e) {
            yLOGGER.log(Level.SEVERE, LOG_PREFIX + "Error building bond trade record", e);
            return false;
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
                        try {
                            String outcome = String.format(
                                "{\"status\":\"%s\",\"t24Ref\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"bloombergId\":\"%s\"}",
                                pStatus, pTransactRef, pMessage, LocalDateTime.now(), pMetadata.bloombergId
                            );
                            CbnTfBackup.backupMessage(outcome, "SC_OUTCOME", pMetadata.originalId);
                            yLOGGER.log(Level.INFO, LOG_PREFIX + "Outcome backup created");
                        } catch (Exception ex) {
                            yLOGGER.log(Level.WARNING, ex,
                                    () -> LOG_PREFIX + "checkOfsResponse: Failed to backup message for BLOOMBERG_ID="
                                            + pMetadata.bloombergId);
                        }
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
    
}