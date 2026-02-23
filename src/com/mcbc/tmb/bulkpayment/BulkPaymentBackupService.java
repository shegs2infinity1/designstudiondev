package com.mcbc.tmb.bulkpayment;

import java.util.List;
import java.util.logging.Logger;

import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbTable;
import com.temenos.t24.api.tables.ebbulkpaymentclearingbackuptmb.*;
import com.temenos.t24.api.records.ebbulkpaymentclearingbackuptmb.*;

/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */
public class BulkPaymentBackupService extends ServiceLifecycle {
    
    private static final Logger LOGGER = Logger.getLogger(BulkPaymentBackupService.class.getName());
    private final Session session;
    private final DataAccess da;
    private final String sessionCompanyId;
    private List<String> retids;
    
    public BulkPaymentBackupService() {
        this.session = new Session(this);
        this.da = new DataAccess(this);
        this.sessionCompanyId = session.getCompanyId();
    }
    
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {
            DatesRecord datesRecord = new DatesRecord(da.getRecord("DATES", sessionCompanyId));
            String t24TransactSystemDate = datesRecord.getToday().getValue();
            String selectionCriteria = "WITH FILE.REC.PROCESS.DATE LT "
                    + t24TransactSystemDate;
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
        // Initialize and read bulk payment clearing record
        EbBulkpaymentClearingTmbRecord bulkPayFileRec = null;
        EbBulkpaymentClearingBackupTmbRecord bulkPayFileBkRec = null;
        try {
            bulkPayFileRec = new EbBulkpaymentClearingTmbRecord(da.getRecord("EB.BULKPAYMENT.CLEARING.TMB", id));
            bulkPayFileBkRec = new EbBulkpaymentClearingBackupTmbRecord(da.getRecord("EB.BULKPAYMENT.CLEARING.TMB", id));
        } catch (Exception e) {
            LOGGER.warning("Error reading record from EB.BULKPAYMENT.CLEARING.TMB " + id + ": " + e.getMessage());
        }
        // Update EB.BULKPAYMENT.CLEARING.TMB if status is valid and record exists
        if (bulkPayFileRec != null) {
            LOGGER.info("Status is "+id);
            EbBulkpaymentClearingBackupTmbTable bulkPayBkTab =  new EbBulkpaymentClearingBackupTmbTable(this);
            EbBulkpaymentClearingTmbTable bulkPayTab = new EbBulkpaymentClearingTmbTable(this);
            try {
                    bulkPayBkTab.write(id, bulkPayFileBkRec);
                    bulkPayTab.delete(id);
                }
           catch (T24IOException e) {
                LOGGER.severe("Error writing record to EB.BULKPAYMENT.CLEARING.TMB " + id + ": " + e.getMessage());
                throw new RuntimeException("Failed to update EB.BULKPAYMENT.CLEARING.TMB for ID: " + id, e);
            }
        }

    }
}
