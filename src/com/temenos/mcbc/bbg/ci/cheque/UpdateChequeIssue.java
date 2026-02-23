package com.temenos.mcbc.bbg.ci.cheque;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.records.chequeissue.ChequeIssueRecord;

/**
 * Auth Routine to Update the status of Frais de Garde after the FT is posted.
 * It is attached to the version FUNDS.TRANSFER,FRAIS.DE.GARDE.BBG.CI
 * 
 * @author shegs
 *
 */
public class UpdateChequeIssue extends RecordLifecycle {

    private final DataAccess da = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        try {
            // Extract FundsTransferRecord from currentRecord
            FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);

            // Extract payment details
            String paydet = ftRec.getPaymentDetails(0).toString();
            String[] alldets = paydet.split("-");

            String chqueid = alldets[1];
            
            System.out.println("Cheque ID is ------------------------------------------------------------------"+chqueid);

            // Fetch Cheque Issue Record
            ChequeIssueRecord chequerec;
            try {
                chequerec = new ChequeIssueRecord(da.getRecord("CIV", "CHEQUE.ISSUE", "", chqueid));
            } catch (Exception e) {
                throw new RuntimeException("Error retrieving Cheque Issue Record for ID: " + chqueid, e);
            }

            // Create TransactionData
            TransactionData txndata = new TransactionData();
            txndata.setCompanyId("CI2250001");
            txndata.setSourceId("CHQ.CHARGE");
            txndata.setTransactionId(chqueid);
            txndata.setNumberOfAuthoriser("0");
            txndata.setFunction("INPUTT");
            txndata.setVersionId("CHEQUE.ISSUE,UPD.STATUS.BBG.CI");

            // Update cheque record status

            chequerec.getLocalRefField("L.STATUS").setValue("YES");
         

            // Add transaction data and updated record
            transactionData.add(txndata);
            currentRecords.add(chequerec.toStructure());

        } catch (Exception e) {
            // Log and handle exceptions
            System.err.println("Error in UpdateChequeIssue: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
