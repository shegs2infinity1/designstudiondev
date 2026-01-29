package com.mcbc.tmb.ws02;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbRecord;
import com.temenos.t24.api.tables.ebwso2paramtmb.EbWso2ParamTmbRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbTable;

/**
 * TODO: Document me!
 *

 *  @author Debesh Dasgupta * /** * @category EB.API - V.UNAUTH.SET.FT.ID.TMB
 * 
 * @param Application
 *            - FUNDS.TRANSFER,INPUT.TMB
 * @param BEFORE
 *            AFTER UNAU ROUTINE - update the EB.WS02.PAYMENT.DETAILS record with the
 *            current FT ID(INAU)
 * @param Version
 *            - FUNDS.TRANSFER,INPUT.TMB
 * @return none

 *
 */
public class tmbUnauthFTIdUpdate extends RecordLifecycle {
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
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        
        
        
       
        ebwsO2PayTable = new EbWs02PaymentDetailsTmbTable(this);
        if (debugg)
            System.out.println("Entering Unauth Routine + tmbUnauthFTIdUpdate");
        try {
            ftRec = new FundsTransferRecord(currentRecord);
            CreditTheirRef = ftRec.getCreditTheirRef().toString();
            ebwsO2DebitCcy = ftRec.getDebitCurrency().toString();
            ebwsO2CreditCcy = ftRec.getCreditCurrency().toString();

//            System.out.println("We are here  ");
            ebwsO2PayRec = new EbWs02PaymentDetailsTmbRecord(
            da.getRecord("EB.WS02.PAYMENT.DETAILS.TMB", CreditTheirRef));

            if (debugg)
                System.out.println("Record Fetched " + ebwsO2PayRec.toString());
            ebwsO2PayRec.setTranId(currentRecordId);

            System.out.println("record w2b :" + ebwsO2PayRec);
            try {
                if (ebwsO2CreditCcy.equals(ebwsO2DebitCcy)) {
//                    System.out.println("Same currency skipping write");
                } else { 
                    ebwsO2PayTable.write(CreditTheirRef, ebwsO2PayRec);
                    /*
                    currentRecord.set(ebwsO2PayRec.toStructure());
                    currentRecords.add(ebwsO2PayRec.toStructure());
                    TransactionData transactions = new TransactionData();
                    System.out.println("We are here  ");
                    transactions.setFunction("INPUT");
                    transactions.setTransactionId(CreditTheirRef);
                    transactions.setNumberOfAuthoriser("0");
                    transactions.setVersionId("EB.WS02.PAYMENT.DETAILS.TMB,INPUT.TMB");
                    transactions.setSourceId(ofsId);
                    transactionData.add(transactions) ;    
                     */                   
//                    System.out.println("We are here after hard updating the Txn to the ws02 rec ");
                 }
            } catch (Exception er) {
                if (debugg)
                    System.out.println("Write Table failing T24IOEXCEPTION  " + er.getMessage());

            }

        } catch (Exception err) {
            System.out.println("Err reading NAU record " + err);
        }
    
        
        
        
        
        
    }
    
    
    
    

}
