package com.mcbc.tmb.ws02;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.templatehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aclockedevents.AcLockedEventsRecord;
import com.temenos.t24.api.records.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author debdas
 * /** * @category EB.API -  V.BAUTH.SET.LOCK.ID.TMB  
 * 
 * @param Application
 *            - AC.LOCKED.EVENTS
 * @param BEFORE AUTH ROUTINE
 *            - update the EB.WS02.PAYMENT.DETAILS record with the current LOCK ID
 * @param Version 
 *            - AC.LOCKED.EVENTS,LOGIREF.API.BLK.FUNDS.TMB.1.0.0
 *
 */
public class tmbUpdateLockingId extends RecordLifecycle {

    boolean debugg = false;
    AcLockedEventsRecord acLockRec;
    EbWs02PaymentDetailsTmbRecord ebwsO2PayRec;
    DataAccess da;
    String TxnRef;

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {
        // TODO Auto-generated method stub

        if (debugg)
            System.out.println("Entering Routine + tmbUpdateLockingId");
        try {
            acLockRec = new AcLockedEventsRecord(currentRecord);
            TxnRef = acLockRec.getTransRef().toString();
        } catch (Exception e) {
            System.out.println("Exception error Reading FT" + e);
        }

        if (debugg)
            System.out.println("Within Routine + tmbBauthSetFTId2");
        try {

            ebwsO2PayRec = new EbWs02PaymentDetailsTmbRecord(da.getRecord("EB.WS02.PAYMENT.DETAILS.TMB", TxnRef));
            if (debugg)
                System.out.println("Record Fetched " + ebwsO2PayRec.toString());
            ebwsO2PayRec.setTranId(currentRecordId);
            currentRecord.set(ebwsO2PayRec.toStructure());

            currentRecords.add(ebwsO2PayRec.toStructure());
            com.temenos.t24.api.complex.eb.templatehook.TransactionData transactions = new com.temenos.t24.api.complex.eb.templatehook.TransactionData();

            transactions.setFunction("INPUT");
            transactions.setTransactionId(TxnRef);
            transactions.setNumberOfAuthoriser("0");
            transactions.setVersionId("EB.WS02.PAYMENT.DETAILS.TMB,INPUT.TMB");
            transactionData.add(transactions);

        } catch (Exception e) {
            System.out.println("Exception error Updating FT" + e);
        }

    }

}
