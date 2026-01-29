package com.mcbc.tmb.bulkpayment;

/**
 * TODO: Document me!
 *
 * @author t.kpohraror
 *
 */
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24IOException;
import java.util.List;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.records.pporderentry.PpOrderEntryRecord;
import com.temenos.t24.api.records.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbRecord;
import com.temenos.t24.api.tables.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbTable;

public class BulkPaymentUpdateRef extends RecordLifecycle {

    String curId = "";
    String lFilename = "";

    /**
     * This method is used to input the current version of the captured record
     * 
     * @category EB.API - V.POE.UPDREF.TMB
     * @param Application                      - EB.BULKPAYMENT.CLEARING.TMB
     * @param TransactionContext.TXN.REFERENCE - To populate the corresponding field
     *                                         with the POE reference
     * @return none
     */

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        PpOrderEntryRecord poeCurrec = new PpOrderEntryRecord(currentRecord);

        curId = currentRecordId;

        lFilename = poeCurrec.getLocalRefField("L.FILE.NAME").getValue();
        System.out.println("curId value:" + curId);
        System.out.println("lFilename value:" + lFilename);
        if (!lFilename.equals("")) {
            DataAccess ddda = new DataAccess(this);
            EbBulkpaymentClearingTmbRecord bulkPayFileRec = new EbBulkpaymentClearingTmbRecord(
                    ddda.getRecord("EB.BULKPAYMENT.CLEARING.TMB", lFilename));
            EbBulkpaymentClearingTmbTable bulkPayTab = new EbBulkpaymentClearingTmbTable(this);

            // System.out.println("bulkPaymentFileRec b4 insert value:" + bulkPayFileRec);

            bulkPayFileRec.setTxnReference(curId);
            bulkPayFileRec.setFileRecordStatus("VALIDATED");

            // bulkPaymentFileRec.toStructure();

            try {
                bulkPayTab.write(lFilename, bulkPayFileRec);
            } catch (T24IOException e2) {
                e2.printStackTrace();
            }

        }

        return poeCurrec.getValidationResponse();
    }

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        System.out.println("***************Starting code**********************************");

        PpOrderEntryRecord poeCurrec = new PpOrderEntryRecord(currentRecord);

        curId = currentRecordId;

        lFilename = poeCurrec.getLocalRefField("L.FILE.NAME").getValue();
        System.out.println("curId value:" + curId);
        System.out.println("lFilename value:" + lFilename);

        if (!lFilename.equals("")) {
            DataAccess ddda = new DataAccess(this);
            EbBulkpaymentClearingTmbRecord bulkPayFileRec = new EbBulkpaymentClearingTmbRecord(
                    ddda.getRecord("EB.BULKPAYMENT.CLEARING.TMB", lFilename));
            EbBulkpaymentClearingTmbTable bulkPayTab = new EbBulkpaymentClearingTmbTable(this);

            // System.out.println("bulkPaymentFileRec b4 insert value:" + bulkPayFileRec);

            bulkPayFileRec.setTxnReference(curId);
            bulkPayFileRec.setFileRecordStatus("PROCESSED");

            // bulkPaymentFileRec.toStructure();

            try {
                bulkPayTab.write(lFilename, bulkPayFileRec);
            } catch (T24IOException e2) {
                e2.printStackTrace();
            }

        }
    }
}