package com.mcbc.tmb.tcsp;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.chequeissue.ChequeIssueRecord;

/**
 * TODO: Document me!
 *
 * @author alyash
 *
 */
public class techcheck extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        ChequeIssueRecord chequeIssue = new ChequeIssueRecord(currentRecord);
        Integer NoChqBooks = Integer.valueOf(chequeIssue.getLocalRefField("NO.OF.CHQBOOKS").getValue());
        if(NoChqBooks > 1){
            

            int firstDotIndex = currentRecordId.indexOf(".");
            String chqType = currentRecordId.substring(0, firstDotIndex);

            int secondDotIndex = currentRecordId.indexOf(".", firstDotIndex + 1);
            String accountId = currentRecordId.substring(firstDotIndex + 1, secondDotIndex);

            int thirdDotIndex = currentRecordId.lastIndexOf(".");
            String lastPart = currentRecordId.substring(thirdDotIndex + 1);

            String serial = lastPart.substring(0, lastPart.length() - 1);
            Integer a = Integer.valueOf(lastPart);
            
            for (int i = 1; i < NoChqBooks; i++) {
                a++;
                String transactionId = chqType + "." + accountId + "." + serial + a.toString();
                TransactionData td = new TransactionData();
                td.setTransactionId(transactionId);
                td.setFunction("INPUT");
                td.setSourceId("BULK.OFS");
                td.setNumberOfAuthoriser("1");
                td.setVersionId("CHEQUE.ISSUE,INPUT.BULK");
                transactionData.add(td);

            }
            
        }
    }
}