package com.temenos.mcbc.bbg.ci.cheque;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.paymentstop.ChargeCodeClass;
import com.temenos.t24.api.records.paymentstop.PaymStopTypeClass;
import com.temenos.t24.api.records.paymentstop.PaymentStopRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.tafj.api.client.impl.T24Context;

/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */

public class PaymentStopCharges extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        PaymentStopRecord paymentRec = new PaymentStopRecord(currentRecord);
        List<PaymStopTypeClass> paymstoptype = paymentRec.getPaymStopType();
        DataAccess da = new DataAccess((T24Context)this);
        FtCommissionTypeRecord ComRec1 = new FtCommissionTypeRecord(da.getRecord("CIV", "FT.COMMISSION.TYPE", "", "STOPCHQ1"));
        String ComAmt1 = ComRec1.getCurrency(0).getFlatAmt().getValue();
        FtCommissionTypeRecord ComRec2 = new FtCommissionTypeRecord(da.getRecord("CIV", "FT.COMMISSION.TYPE", "", "STOPCHQ2"));
        String ComAmt2 = ComRec2.getCurrency(0).getFlatAmt().getValue();
        
        
        System.out.println("Current Record: " + currentRecord.toString());
        System.out.println("Payment Stop Type: " + paymstoptype);
        System.out.println("Payment Stop multivalue size: " + paymstoptype.size());

        for (int i = 0; i < paymstoptype.size(); i++) {
            try {
                PaymStopTypeClass stoptype = paymentRec.getPaymStopType(i);
                String numCheques = stoptype.getNoOfLeaves().getValue();
                List<ChargeCodeClass> chargeCodeList = stoptype.getChargeCode();

                if (numCheques == null || numCheques.isEmpty()) {
                    System.out.println("NoOfLeaves value is missing for stop type at index " + i);
                    continue;
                }
                
                int numCheque;
                try {
                    numCheque = Integer.parseInt(numCheques);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number format for cheques: " + numCheques);
                    continue;
                }
                
                if (chargeCodeList == null || chargeCodeList.isEmpty()) {
                    System.out.println("Charge code list is empty for Payment Stop Type");

                    ChargeCodeClass chargecode = new ChargeCodeClass();
                    if (numCheque == 1) {
                        chargecode.setChargeCode("STOPCHQ1");
                        chargecode.setChgAmount(ComAmt1);
                    } else {
                        chargecode.setChargeCode("STOPCHQ2");
                        chargecode.setChgAmount(ComAmt2);
                    }

                    // Set charge code at index `i`
                    
                    stoptype.setChargeCode(chargecode, i);
                } else
                {
                    ChargeCodeClass chargecode;
                    if (i < chargeCodeList.size()) {
                        chargecode = chargeCodeList.get(i);
                    } else {
                        chargecode = new ChargeCodeClass();
                    }
                    if (numCheque == 1) {
                        chargecode.setChargeCode("STOPCHQ1");
                        chargecode.setChgAmount(ComAmt1);
                    } else {
                        chargecode.setChargeCode("STOPCHQ2");
                        chargecode.setChgAmount(ComAmt2);
                    }
                    ;
                    stoptype.setChargeCode(chargecode, i);
                }
        
                paymentRec.setPaymStopType(stoptype, i);

            } catch (Exception e) {
                System.err.println("Error processing Payment Stop Type: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Final Payment Stop Record: " + paymentRec);
        currentRecord.set(paymentRec.toStructure());
    }
}
