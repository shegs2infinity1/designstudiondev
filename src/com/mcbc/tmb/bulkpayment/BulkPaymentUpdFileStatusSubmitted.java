package com.mcbc.tmb.bulkpayment;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.records.ebbulkpaymenthdrtrltmb.EbBulkpaymentHdrTrlTmbRecord;
import com.temenos.t24.api.tables.ebbulkpaymenthdrtrltmb.EbBulkpaymentHdrTrlTmbTable;

/**
 * TODO: Document me!
 *
 * @author t.kpohraror
 *
 */
public class BulkPaymentUpdFileStatusSubmitted extends RecordLifecycle {

    String curId = "";
    String lFilename = "";

    /**
     * This method is used to update the file status of the BulkPayment Header
     * record
     * 
     * @category EB.API - V.UPD.BULKFILESTATUS.SUBM.TMB
     * @param Application - EB.BULKPAYMENT.CLEARING.TMB
     * @return none
     */

    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        System.out.println("***************Starting code**********************************");

        EbBulkpaymentClearingTmbRecord bulkPayFileRec = new EbBulkpaymentClearingTmbRecord(currentRecord);

        curId = currentRecordId;

        lFilename = bulkPayFileRec.getBulkFileName().getValue();
        System.out.println("curId value:" + curId);
        System.out.println("lFilename value:" + lFilename);

        DataAccess ddda = new DataAccess(this);
        EbBulkpaymentHdrTrlTmbRecord bulkPayFileHdrTrlRec = new EbBulkpaymentHdrTrlTmbRecord(
                ddda.getRecord("EB.BULKPAYMENT.HDR.TRL.TMB", lFilename));
        EbBulkpaymentHdrTrlTmbTable bulkPayHdrTrlTab = new EbBulkpaymentHdrTrlTmbTable(this);

        // System.out.println("bulkPayFileHdrTrlRec b4 insert value:" + bulkPayFileHdrTrlRec);

        bulkPayFileHdrTrlRec.setFileStatus("SUBMITTED");

       
        try {
            bulkPayHdrTrlTab.write(lFilename, bulkPayFileHdrTrlRec);
        } catch (T24IOException e2) {
            e2.printStackTrace();
        }

        // System.out.println("bulkPayFileHdrTrlRec after insert value:" + bulkPayFileHdrTrlRec);
    }
}
