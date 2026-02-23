
package com.mcbc.tmb.ws02;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.templatehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbRecord;
import com.temenos.t24.api.tables.ebwso2paramtmb.EbWso2ParamTmbRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbTable;

/**
 * TODO: Document me!
 *
 * @author debdas * /** * @category EB.API - V.BAUTH.SET.FT.ID.TMB
 * 
 * @param Application
 *            - FUNDS.TRANSFER,INPUT.TMB
 * @param BEFORE
 *            AUTH ROUTINE - update the EB.WS02.PAYMENT.DETAILS record with the
 *            current FT ID
 * @param Version
 *            - FUNDS.TRANSFER,INPUT.TMB
 * @return none
 *
 *
 */

public class tmbBauthSetFTId extends RecordLifecycle {
    boolean debugg = false;
    String ftId;
    String wsO2PaymentRec;
    FundsTransferRecord ftRec;
    EbWs02PaymentDetailsTmbRecord ebwsO2PayRec;
    EbWs02PaymentDetailsTmbTable ebwsO2PayTable;
    EbWso2ParamTmbRecord ebwsO2ParamRec;
    String CreditTheirRef;
    DataAccess da = new DataAccess(this);
    String ebwsO2InterfaceType;
    String ebwsO2TransferType;
    String ebwsO2ParamId;
    String ebwsO2MsgId;
    String ebwsO2DebitAcc;
    String ebwsO2DebitCcy;
    String ebwsO2CreditAcc;
    String ebwsO2CreditCcy;
    String ebwsO2CDebitAmt;
    String ebwsO2CCreditAmt;
    String ebwsO2PaymentDetails;
    String versionId;
    String ofsId = "BULK.OFS";

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<com.temenos.t24.api.complex.eb.templatehook.TransactionData> transactionData,
            List<TStructure> currentRecords) {
        if (debugg)
            System.out.println("Entering Routine + tmbBauthSetFTId");
        try {
            ftRec = new FundsTransferRecord(currentRecord);
            CreditTheirRef = ftRec.getCreditTheirRef().toString();

        } catch (Exception e) {
            System.out.println("Exception error readingTxn " + e);
        }

        if (debugg)
            System.out.println("Within Routine + tmbBauthSetFTId2");
        try {

            ebwsO2PayRec = new EbWs02PaymentDetailsTmbRecord(
                    da.getRecord("EB.WS02.PAYMENT.DETAILS.TMB", CreditTheirRef));
            if (debugg)
                System.out.println("Record Fetched " + ebwsO2PayRec.toString());

            ebwsO2PayRec.setTranId(currentRecordId);
            currentRecord.set(ebwsO2PayRec.toStructure());

            currentRecords.add(ebwsO2PayRec.toStructure());
            TransactionData transactions = new TransactionData();
//            System.out.println("We are here  ");
            transactions.setFunction("INPUT");
            transactions.setTransactionId(CreditTheirRef);
            transactions.setNumberOfAuthoriser("0");
            transactions.setVersionId("EB.WS02.PAYMENT.DETAILS.TMB,INPUT.TMB");
            transactionData.add(transactions);
//            System.out.println("We are here 2 ");
        } catch (Exception e) {
            System.out.println("Exception error Updating FT" + e);
        }

    }
}
