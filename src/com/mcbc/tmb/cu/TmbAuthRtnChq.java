package com.mcbc.tmb.cu;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.chequeissue.ChequeIssueRecord;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author alyash
 *
 */
/**
 * Modification Details
 * 04-06-2025/Ranjith/Fixed the cheque id issue for serial number higher than 9.
 * 18-08-2025/Ranjith/Mapped the local fields to the all cheque issue records.
 *
 */
public class TmbAuthRtnChq extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        ChequeIssueRecord chequeIssue = new ChequeIssueRecord(currentRecord);
        Integer NoChqBooks = Integer.valueOf(chequeIssue.getLocalRefField("NO.OF.CHQBOOKS").getValue());
        Session ss = new Session(this);
        String curUser = ss.getUserId();
        
        if (NoChqBooks > 1) {

            int firstDotIndex = currentRecordId.indexOf(".");
            String chqType = currentRecordId.substring(0, firstDotIndex);

            int secondDotIndex = currentRecordId.indexOf(".", firstDotIndex + 1);
            String accountId = currentRecordId.substring(firstDotIndex + 1, secondDotIndex);

            int thirdDotIndex = currentRecordId.lastIndexOf(".");
            String lastPart = currentRecordId.substring(thirdDotIndex + 1);

            String serial = lastPart.substring(0, lastPart.length() - 1);
            Integer a = Integer.valueOf(lastPart);
//20250818 - S Cheque issue fields update
            ChequeIssueRecord chequeIssueRec = new ChequeIssueRecord(this);
            chequeIssueRec.getLocalRefField("L.CROSSING.TYPE").setValue(chequeIssue.getLocalRefField("L.CROSSING.TYPE").getValue());
            chequeIssueRec.getLocalRefField("L.NAME.ON.CHQ").setValue(chequeIssue.getLocalRefField("L.NAME.ON.CHQ").getValue());
            chequeIssueRec.getLocalRefField("L.ADD.BRANCH").setValue(chequeIssue.getLocalRefField("L.ADD.BRANCH").getValue());
            chequeIssueRec.getLocalRefField("L.COLL.CENTRE").setValue(chequeIssue.getLocalRefField("L.COLL.CENTRE").getValue());
            chequeIssueRec.getLocalRefField("L.PRINT.COMP").setValue(chequeIssue.getLocalRefField("L.PRINT.COMP").getValue());
            chequeIssueRec.getLocalRefField("L.CHQ.CO.CODE").setValue(chequeIssue.getLocalRefField("L.CHQ.CO.CODE").getValue());
            chequeIssueRec.getWaiveCharges().setValue(chequeIssue.getWaiveCharges().getValue());
//20250818 - E             
            for (int i = 1; i < NoChqBooks; i++) {
                a++;
                // 20250604 - S - Fixed the serail number issue
                // String transactionId = chqType + "." + accountId + "." +
                // serial + a.toString();
                String transactionId = String.valueOf(chqType) + "." + accountId + "." + String.format("%07d", a);
                // 20250604 - E - Fixed the serail number issue
                TransactionData td = new TransactionData();
                td.setTransactionId(transactionId);
                td.setFunction("INPUT");
                td.setSourceId("BULK.OFS");              
                td.setNumberOfAuthoriser("1");
                td.setUserName(curUser);
                td.setVersionId("CHEQUE.ISSUE,INPUT.BULK");
                transactionData.add(td);
                
//                System.out.println("final message" + td);
//20250818 - S 
                currentRecords.add(chequeIssueRec.toStructure());
//20250818 - E                 
            }

        }
    }
}
