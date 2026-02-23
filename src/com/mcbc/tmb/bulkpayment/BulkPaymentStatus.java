package com.mcbc.tmb.bulkpayment;

import java.util.List;
import java.util.logging.Logger;

import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbRecord;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbTable;
import com.temenos.t24.api.tables.ebbulkpaymentstatus.EbBulkpaymentStatusRecord;
import com.temenos.t24.api.tables.ebbulkpaymentstatus.EbBulkpaymentStatusTable;

/**
 * Service to select failed records from EB.BULKPAYMENT.CLEARING.TMB
 * and update a concat table with details from OFS.REQUEST.DETAIL
 * @author shegs
 */
public class BulkPaymentStatus extends ServiceLifecycle {
    
    private static final Logger LOGGER = Logger.getLogger(BulkPaymentStatus.class.getName());
    private final Session session;
    private final DataAccess da;
    private final String sessionCompanyId;
    private List<String> retids;

    public BulkPaymentStatus() {
        this.session = new Session(this);
        this.da = new DataAccess(this);
        this.sessionCompanyId = session.getCompanyId();
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {
            DatesRecord datesRecord = new DatesRecord(da.getRecord("DATES", sessionCompanyId));
            String t24TransactSystemDate = datesRecord.getToday().getValue();
            String selectionCriteria = "WITH FILE.RECORD.STATUS EQ PROCESSING AND FILE.REC.PROCESS.DATE EQ "
                    + t24TransactSystemDate;
            
            LOGGER.info("Selection Criteria " + selectionCriteria);
            
            retids = da.selectRecords("", "EB.BULKPAYMENT.CLEARING.TMB", "", selectionCriteria);
            LOGGER.info("Successfully retrieved " + retids.size() + " records for processing");
            return retids;
        } catch (Exception e) {
            LOGGER.severe("Error retrieving records from EB.BULKPAYMENT.CLEARING.TMB: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve bulk payment records", e);
        }
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        // Flags and variables
        boolean ofsrec = false;
        String status = null;
        String msgIn = null;
        String msgOut = null;
        String transref = null;

        // Retrieve OFS request details
        try {
            OfsRequestDetailRecord ordRec = new OfsRequestDetailRecord(da.getRecord("OFS.REQUEST.DETAIL", id));
            ofsrec = true;
            status = ordRec.getStatus().toString();
            msgIn = ordRec.getMsgIn().toString();
            msgOut = ordRec.getMsgOut().toString();
            transref = ordRec.getTransReference().toString();
        } catch (Exception e) {
            LOGGER.severe("Error reading OFS record " + id + ": " + e.getMessage());
        }

        // Initialize and read bulk payment clearing record
        EbBulkpaymentClearingTmbRecord bulkPayFileRec = null;
        try {
            bulkPayFileRec = new EbBulkpaymentClearingTmbRecord(da.getRecord("EB.BULKPAYMENT.CLEARING.TMB", id));

        } catch (Exception e) {
            LOGGER.warning("Error reading record from EB.BULKPAYMENT.CLEARING.TMB " + id + ": " + e.getMessage());
        }

        // Update EB.BULKPAYMENT.CLEARING.TMB if status is valid and record exists
        if (bulkPayFileRec != null && status != null) {
            LOGGER.info("Status is "+status);
            EbBulkpaymentClearingTmbTable bulkPayTab = new EbBulkpaymentClearingTmbTable(this);
            try {
                if ("PROCESSED".equals(status)) {
                    bulkPayFileRec.setFileRecordStatus("PROCESSED");
                    bulkPayFileRec.setTxnReference(transref);
                    bulkPayTab.write(id, bulkPayFileRec);
                    LOGGER.info("Successfully updated EB.BULKPAYMENT.CLEARING.TMB: " + id);
                } else if ("ERROR".equals(status)) {
                    bulkPayFileRec.setFileRecordStatus("ERROR");
                    bulkPayFileRec.setErrorInfo(msgOut);
                    bulkPayTab.write(id, bulkPayFileRec);
                    LOGGER.info("Successfully updated EB.BULKPAYMENT.CLEARING.TMB: " + id);
                }
            } catch (T24IOException e) {
                LOGGER.severe("Error writing record to EB.BULKPAYMENT.CLEARING.TMB " + id + ": " + e.getMessage());
                throw new RuntimeException("Failed to update EB.BULKPAYMENT.CLEARING.TMB for ID: " + id, e);
            }
        }

        // Initialize and read bulk payment status record
        EbBulkpaymentStatusTable payTable = new EbBulkpaymentStatusTable(this);
        EbBulkpaymentStatusRecord payStat = new EbBulkpaymentStatusRecord();

        // Update record fields only if payStat is empty/new and OFS record was read
        if (ofsrec && status != null) {
            try {
                payStat.setMsgIn(msgIn);
                payStat.setMsgOut(msgOut);
                payStat.setStatus(status);
                payTable.write(id, payStat);
                LOGGER.info("Successfully updated bulk payment status for ID: " + id);
            } catch (T24IOException e) {
                LOGGER.severe("Error writing record " + id + ": " + e.getMessage());
                throw new RuntimeException("Failed to update bulk payment status for ID: " + id, e);
            }
        } else {
            LOGGER.info("Skipping update for ID: " + id + " as record already exists or no valid OFS data");
        }
    }
}