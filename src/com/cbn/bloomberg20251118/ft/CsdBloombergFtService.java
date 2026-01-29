package com.cbn.bloomberg.ft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cbn.bloomberg.ft.CsdBloombergFtMapper.FileItemRef;
import com.cbn.bloomberg.ft.CsdBloombergFtMapper.MqItemRef;
import com.cbn.bloomberg.hp.CsdBloombergTafjLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temenos.api.TBoolean;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * Title: CsdBloombergFtService.java Author: CSD Development Team Date Created:
 * 2025-10-11
 *
 * Purpose: Bloomberg FT Service Hook supporting dual ingestion modes: FILE or
 * WMQ (IBM MQ). The ADP_FLAG constant controls the data source.
 * 
 * FILE mode: Scans JSON files from a directory, extracts transaction IDs, and
 * moves processed files to an archive directory. WMQ mode: Consumes messages
 * from IBM MQ queue and processes them.
 *
 * Usage: This service is invoked by T24 Service Framework. It implements the
 * ServiceLifecycle interface with getIds() and updateRecord() methods.
 *
 */
public class CsdBloombergFtService extends ServiceLifecycle {

    // TAFJ Custom LOG Details
    private static final Logger LOG = CsdBloombergTafjLogger.forClass(CsdBloombergFtService.class);
    private static final String ADP_FLAG = "FILE";
    private static final String OFS_EDL = "/-1/NO,";
    private static final String OFS_FNC = "INPUT";
    private static final String OFS_SRC = "OFS.BMRG";
    private static final String OFS_VSN = "FUNDS.TRANSFER,CBN.BKSD.FMD";

    private static final String FILE_PATN = "*.json";
    private static final Path BMRG_DIR = Paths.get("/t24app/app/bnk/UD/BLOOMBERG/");
    private static final Path JSON_DIR = Paths.get(BMRG_DIR.toString(), "IN/FT");
    private static final Path PROC_DIR = Paths.get(BMRG_DIR.toString(), "DONE/FT");
    private static final Path EXCP_DIR = Paths.get(BMRG_DIR.toString(), "ERROR/FT");

    // Shared components
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final CsdBloombergFtPayload payloadHandler = new CsdBloombergFtPayload(OBJECT_MAPPER);
    private final CsdBloombergFtProducer producer = new CsdBloombergFtProducer();

    String companyId = "NG0010001";
    Session ss = new Session(this);
    DataAccess da = new DataAccess(this);

    /**
     * Retrieves the list of transaction IDs to be processed by the service. Temenos
     * core method - signature must not be changed.
     *
     * This interface is invoked from BATCH.JOB.CONTROL in place of the SELECT
     * routine. BATCH.BUILD.LIST will be invoked for the prepared list of Ids.
     */
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> ids = new ArrayList<>();

        LOG.log(Level.INFO, "[CsdBloombergFtService] getIds: ADP_FLAG={0}", ADP_FLAG);

        if ("FILE".equalsIgnoreCase(ADP_FLAG)) {
            ids.addAll(processFileMode());
        } else if ("WMQ".equalsIgnoreCase(ADP_FLAG)) {
            ids.addAll(processWmqMode());
        } else {
            LOG.log(Level.SEVERE, "[CsdBloombergFtService] ERROR: Unknown ADP_FLAG={0}. Valid values: FILE, WMQ",
                    ADP_FLAG);
        }

        LOG.log(Level.INFO, "[CsdBloombergFtService] getIds: total queued IDs={0}", ids.size());
        return ids;
    }

    /**
     * Processes FILE mode: scans directory for JSON files and extracts IDs.
     */
    private List<String> processFileMode() {
        List<String> ids = new ArrayList<>();

        LOG.log(Level.INFO, "[CsdBloombergFtService] FILE mode: scanning {0}", JSON_DIR);

        if (!Files.isDirectory(JSON_DIR)) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] JSON directory not found: {0}", JSON_DIR);
            return ids;
        }

        ids.addAll(CsdBloombergFtMapper.extractIdsFromDirectoryAndMove(JSON_DIR, FILE_PATN, PROC_DIR, OBJECT_MAPPER));

        LOG.log(Level.INFO, "[CsdBloombergFtService] FILE mode: total queued IDs={0}", ids.size());
        return ids;
    }

    /**
     * Processes WMQ mode: consumes messages from IBM MQ queue.
     */
    private List<String> processWmqMode() {
        List<String> ids = new ArrayList<>();

        LOG.log(Level.INFO, "[CsdBloombergFtService] WMQ mode: consuming messages from MQ");

        ids.addAll(CsdBloombergFtMapper.extractIdsFromMq(OBJECT_MAPPER));

        LOG.log(Level.INFO, "[CsdBloombergFtService] WMQ mode: total queued IDs={0}", ids.size());
        return ids;
    }

    /**
     * Updates a single FundsTransfer record based on the provided transaction ID.
     * Temenos core method - signature must not be changed.
     *
     * This interface enables the developer to update the records of any table. The
     * update will be attempted before control is returned to the service dispatcher
     * BATCH.JOB.CONTROL. This interface is invoked from BATCH.JOB.CONTROL in place
     * of the RECORD routine.
     *
     * After submitting the transaction, retrieves the OFS response from
     * OFS.REQUEST.DETAIL to get the actual T24 transaction reference.
     */
    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        companyId = ss.getCompanyId();
        LOG.log(Level.INFO, "[CsdBloombergFtService] updateRecord: processing id={0}, ADP_FLAG={1}, companyId={2}",
                new Object[] { id, ADP_FLAG, companyId });

        String transactionRef = "";
        String status = "failure";
        String message = "Unknown error";
        JsonNode originalItem = null;

        try {
            JsonNode item = null;
            String source = "";

            // Step 1: Retrieve the item based on adapter mode
            if ("FILE".equalsIgnoreCase(ADP_FLAG)) {
                item = processFileRecord(id);
                if (item != null) {
                    source = "FILE:" + id;
                }
            } else if ("WMQ".equalsIgnoreCase(ADP_FLAG)) {
                item = processWmqRecord(id);
                if (item != null) {
                    source = "WMQ:" + id;
                }
            } else {
                LOG.log(Level.SEVERE, "[CsdBloombergFtService] ERROR: Unknown ADP_FLAG={0}", ADP_FLAG);
                message = "Unknown adapter flag: " + ADP_FLAG;
                publishResponse(id, status, message, transactionRef, null);
                return;
            }

            if (item == null) {
                LOG.log(Level.WARNING, "[CsdBloombergFtService] No item found for id={0}", id);
                message = "Item not found or invalid FUNDS_MOVEMENTS";
                persistToExcepts(id, message);
                publishResponse(id, status, message, transactionRef, null);
                return;
            }

            originalItem = item;
            LOG.log(Level.FINE, "[CsdBloombergFtService] Processing item from {0}: {1}", new Object[] { source, item });

            // Step 2: Map JSON to FT field map
            Map<String, String> data = CsdBloombergFtMapper.mapFundsMovementToFt(item);
            if (data == null || data.isEmpty()) {
                LOG.log(Level.WARNING, "[CsdBloombergFtService] Mapping returned no data for id={0}", id);
                message = "Mapping returned no data";
                persistToExcepts(id, message);
                publishResponse(id, status, message, transactionRef, originalItem);
                return;
            }

            LOG.log(Level.FINE, "[CsdBloombergFtService] Mapped data: {0}", data);

            // Step 3: Build responseId based on adapter mode
            String responseId = buildResponseId(id);
            LOG.log(Level.INFO, "[CsdBloombergFtService] Built responseId: {0}", responseId);

            // Step 4: Build and populate FT record - this submits to T24
            boolean success = buildFundsTransferRecord(responseId, data, transactionData, records);

            if (!success) {
                message = "Validation failed while building FundsTransferRecord";
                persistToExcepts(id, message);
                publishResponse(id, status, message, transactionRef, originalItem);
                return;
            }
            LOG.log(Level.INFO, "[CsdBloombergFtService] FundsTransferRecord built and submitted to T24 for id={0}",
                    id);

            // Step 5: Retrieve OFS response from OFS.REQUEST.DETAIL
            String ofsResponse = retrieveOfsResponse(responseId);

            if (ofsResponse != null && !ofsResponse.isEmpty()) {
                LOG.log(Level.INFO, "[CsdBloombergFtService] OFS Response: {0}", ofsResponse);

                // Check for OFS errors
                if (ofsResponse.contains(OFS_EDL)) {
                    status = "failure";
                    message = "T24 OFS Error: " + ofsResponse;
                    transactionRef = "ERROR";
                    LOG.log(Level.WARNING, "[CsdBloombergFtService] OFS Error detected: {0}", ofsResponse);
                } else {
                    // Parse transaction reference from OFS response
                    transactionRef = parseTransactionRef(ofsResponse);
                    status = "success"; 
                    message = "Funds Transfer processed successfully";
                    LOG.log(Level.INFO, "[CsdBloombergFtService] Transaction successful: ref={0}", transactionRef);
                }
            } else {
                // No response available yet
                status = "success";
                message = "Funds Transfer submitted successfully";
                transactionRef = responseId;
                LOG.log(Level.INFO, "[CsdBloombergFtService] Transaction submitted, response pending");
            }

            // Step 6: Publish response
            publishResponse(id, status, message, transactionRef, originalItem);

            // Step 7: Acknowledge MQ message on success
            if ("success".equalsIgnoreCase(status) && "WMQ".equalsIgnoreCase(ADP_FLAG)) {
                CsdBloombergFtMapper.acknowledgeMqMessage(id);
                LOG.log(Level.INFO, "[CsdBloombergFtService] MQ message acknowledged for id={0}", id);
            }

        } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
            LOG.log(Level.SEVERE, "[CsdBloombergFtService] JSON parse error for id=" + id, jpe);
            message = "JSON parse error: " + jpe.getMessage();
            persistToExcepts(id, message);
            publishResponse(id, status, message, transactionRef, originalItem);
        } catch (java.io.IOException ioe) {
            LOG.log(Level.SEVERE, "[CsdBloombergFtService] I/O error for id=" + id, ioe);
            message = "I/O error: " + ioe.getMessage();
            persistToExcepts(id, message);
            publishResponse(id, status, message, transactionRef, originalItem);
        } catch (RuntimeException re) {
            LOG.log(Level.SEVERE, "[CsdBloombergFtService] Runtime error for id=" + id, re);
            message = "Runtime error: " + re.getMessage();
            persistToExcepts(id, message);
            publishResponse(id, status, message, transactionRef, originalItem);
        }
    }

    /**
     * Builds the responseId for T24 based on the adapter mode.
     *
     * For WMQ mode: Uses the JMS Message ID from the incoming message For FILE
     * mode: Uses the file name prefix
     *
     * @param id The composite ID (e.g.,
     *           "WMQ|ID:414d512051d45f424c4f4f4d4245524716740b690137004d|FM|0")
     * @return The responseId to use for T24 OFS.REQUEST.DETAIL lookup
     */
    private String buildResponseId(String id) {
        if ("WMQ".equalsIgnoreCase(ADP_FLAG)) {
            // Extract JMS Message ID from composite ID
            // Format: "WMQ|<JMS_MESSAGE_ID>|FM|<index>"
            MqItemRef ref = CsdBloombergFtMapper.parseMqItemRef(id);
            if (ref != null && ref.messageId() != null) {
                // Use the JMS Message ID and strip "ID:" prefix if present
                String jmsMessageId = ref.messageId();
                if (jmsMessageId.startsWith("ID:")) {
                    jmsMessageId = jmsMessageId.substring(3);
                }
                LOG.log(Level.INFO, "[CsdBloombergFtService] Using JMS Message ID as responseId: {0}", jmsMessageId);
                return jmsMessageId;
            }
        } else if ("FILE".equalsIgnoreCase(ADP_FLAG)) {
            // For FILE mode, use the file name prefix
            return extractPrefix(id);
        }

        // Fallback
        return extractPrefix(id);
    }

    /**
     * Retrieves the OFS response from OFS.REQUEST.DETAIL table. Uses the same
     * approach as BulkPaymentFileProcessService.
     */
    private String retrieveOfsResponse(String responseId) {
        try {
            TBoolean exists = null;
            OfsRequestDetailRecord ofsRequestDetailRecord = da.getRequestResponse(responseId, exists);
            String msgOut = ofsRequestDetailRecord.getMsgOut().getValue();
            LOG.log(Level.FINE, "[CsdBloombergFtService] Message Out Response={0}", msgOut);
            if (msgOut == null || msgOut.isEmpty()) {
                // Fallback: try reading directly from OFS.REQUEST.DETAIL
                try {
                    TStructure ofsRequestDetailTRecord = da.getRecord("OFS.REQUEST.DETAIL", responseId);
                    ofsRequestDetailRecord = new OfsRequestDetailRecord(ofsRequestDetailTRecord);
                    msgOut = ofsRequestDetailRecord.getMsgOut().getValue();
                } catch (Exception e) {
                    LOG.log(Level.FINE,
                            "[CsdBloombergFtService] Could not retrieve OFS.REQUEST.DETAIL for responseId={0}",
                            responseId);
                }
            }

            return msgOut;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] Error retrieving OFS response for responseId=" + responseId,
                    e);
            return null;
        }
    }

    /**
     * Parses the transaction reference from the OFS response. OFS response format
     * typically contains the transaction ID after successful processing.
     */
    private String parseTransactionRef(String ofsResponse) {
        try {
            // OFS response format: //COMPANY_ID/TRANSACTION_ID,FIELD:VALUE,...
            // Example: //CD2433000/FT2512345678,DEBIT.ACCT.NO:1234567890,...
            if (ofsResponse.contains("/") && ofsResponse.contains(",")) {
                int startIdx = ofsResponse.lastIndexOf("/") + 1;
                int endIdx = ofsResponse.indexOf(",", startIdx);
                if (startIdx > 0 && endIdx > startIdx) {
                    String txnRef = ofsResponse.substring(startIdx, endIdx);
                    LOG.log(Level.INFO, "[CsdBloombergFtService] Parsed transaction reference: {0}", txnRef);
                    return txnRef;
                }
            }

            // If parsing fails, return the full response (truncated)
            return ofsResponse.length() > 50 ? ofsResponse.substring(0, 50) : ofsResponse;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] Error parsing transaction reference from OFS response", e);
            return "UNKNOWN";
        }
    }

    /**
     * Builds response and publishes using CsdBloombergFtPayload and
     * CsdBloombergFtProducer.
     */
    private void publishResponse(String id, String status, String message, String transactionRef,
            JsonNode originalItem) {
        try {
            // Build JSON response using payload handler
            String jsonResponse = payloadHandler.buildResponse(status, message, transactionRef, originalItem);

            LOG.log(Level.INFO, "[CsdBloombergFtService] Built response: {0}", jsonResponse);

            // Publish using producer
            producer.publishResponse(jsonResponse, ADP_FLAG, id);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[CsdBloombergFtService] Error publishing response for id=" + id, e);
        }
    }

    /**
     * Persist the failed transaction source (FILE or WMQ) to EXCEPTS directory.
     */
    private void persistToExcepts(String id, String reason) {
        try {
            if ("FILE".equalsIgnoreCase(ADP_FLAG)) {
                FileItemRef ref = CsdBloombergFtMapper.parseFileItemRef(id);
                if (ref != null) {
                    CsdBloombergFtMapper.persistFailedFileItem(ref, EXCP_DIR, reason);
                } else {
                    LOG.log(Level.WARNING,
                            "[CsdBloombergFtService] persistToExcepts: could not parse FILE ref from id={0}", id);
                }
            } else if ("WMQ".equalsIgnoreCase(ADP_FLAG)) {
                MqItemRef ref = CsdBloombergFtMapper.parseMqItemRef(id);
                if (ref != null) {
                    CsdBloombergFtMapper.persistFailedMqItem(ref, EXCP_DIR, reason);
                } else {
                    LOG.log(Level.WARNING,
                            "[CsdBloombergFtService] persistToExcepts: could not parse WMQ ref from id={0}", id);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[CsdBloombergFtService] persistToExcepts failed for id=" + id, ex);
        }
    }

    /**
     * Processes a FILE mode record.
     */
    private JsonNode processFileRecord(String p_id) throws java.io.IOException {
        FileItemRef ref = CsdBloombergFtMapper.parseFileItemRef(p_id);
        if (ref == null) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] FILE mode: unexpected id format: {0}", p_id);
            return null;
        }

        JsonNode root = CsdBloombergFtMapper.readRoot(ref.file(), OBJECT_MAPPER);
        if (!CsdBloombergFtMapper.hasFundsMovements(root)) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] FILE mode: no FUNDS_MOVEMENTS in {0}",
                    ref.file().getFileName());
            return null;
        }

        return CsdBloombergFtMapper.getFundsMovementAt(root, ref.index());
    }

    /**
     * Processes a WMQ mode record.
     */
    private JsonNode processWmqRecord(String p_id) throws java.io.IOException {
        MqItemRef ref = CsdBloombergFtMapper.parseMqItemRef(p_id);
        if (ref == null) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] WMQ mode: unexpected id format: {0}", p_id);
            return null;
        }

        JsonNode root = CsdBloombergFtMapper.readMqMessage(ref.messageId(), OBJECT_MAPPER);
        if (root == null || !CsdBloombergFtMapper.hasFundsMovements(root)) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] WMQ mode: no FUNDS_MOVEMENTS in message {0}",
                    ref.messageId());
            return null;
        }

        return CsdBloombergFtMapper.getFundsMovementAt(root, ref.index());
    }

    /**
     * Builds a FundsTransferRecord from the mapped data.
     */
    private boolean buildFundsTransferRecord(String responseId, Map<String, String> p_data,
            List<SynchronousTransactionData> p_transactionData, List<TStructure> p_records) {

        // Extract fields
        String drAccount = p_data.getOrDefault("DACC", "");
        String drCurrency = p_data.getOrDefault("DCCY", "");
        String drAmount = p_data.getOrDefault("DAMT", "");
        String drValueDate = p_data.getOrDefault("DDAT", "");
        String pyDetails = p_data.getOrDefault("PDET", "");
        String odCustomer = p_data.getOrDefault("OCUS", "");
        String crAccount = p_data.getOrDefault("CACC", "");
        String crCurrency = p_data.getOrDefault("CCCY", "");

        // Validate required fields
        if (drAccount.isEmpty() || drCurrency.isEmpty() || drAmount.isEmpty() || crAccount.isEmpty()
                || crCurrency.isEmpty()) {
            LOG.log(Level.SEVERE, "[CsdBloombergFtService] Missing required fields");
            return false;
        }
        // Log extracted field values
        LOG.log(Level.INFO,
                "[CsdBloombergFtService] Extracted fields - DACC={0}, DCCY={1}, DAMT={2}, DDAT={3}, PDET={4}, OCUS={5}, CACC={6}, CCCY={7}",
                new Object[] { drAccount, drCurrency, drAmount, drValueDate, pyDetails, odCustomer, crAccount,
                        crCurrency });

        // Create FundsTransfer record
        FundsTransferRecord ftRecord = new FundsTransferRecord();
        ftRecord.setDebitAcctNo(drAccount);
        ftRecord.setDebitCurrency(drCurrency);
        ftRecord.setDebitAmount(drAmount);
        ftRecord.setDebitValueDate(drValueDate);
        ftRecord.setPaymentDetails(pyDetails, 0);
        ftRecord.setOrderingCust(odCustomer, 0);
        ftRecord.setCreditAcctNo(crAccount);
        ftRecord.setCreditCurrency(crCurrency);
        LOG.log(Level.INFO, "[CsdBloombergFtService] Adding record: {0}", ftRecord);

        // Create transaction envelope with responseId
        SynchronousTransactionData txnData = new SynchronousTransactionData();
        txnData.setResponseId(responseId);
        txnData.setCompanyId(companyId);
        txnData.setVersionId(OFS_VSN);
        txnData.setFunction(OFS_FNC);
        txnData.setNumberOfAuthoriser("0");
        txnData.setSourceId(OFS_SRC);
        p_transactionData.add(txnData);
        p_records.add(ftRecord.toStructure());
        return true;
    }

    /**
     * Extracts prefix from transaction ID for response identification (FILE mode
     * fallback).
     */
    public static String extractPrefix(String input) {
        try {
            String[] segments = input.split("\\|");
            if (segments.length < 2) {
                return "UNKNOWN";
            }
            String filePath = segments[1];
            int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            String fileName = lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
            int dashIndex = fileName.indexOf('-');
            return dashIndex > 0 ? fileName.substring(0, dashIndex) : fileName;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[CsdBloombergFtService] Error extracting prefix from: " + input, e);
            return "UNKNOWN";
        }
    }
}