package com.temenos.mcbc.bbg.ci.cheque;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.records.paymentstop.ChargeCodeClass;
import com.temenos.t24.api.records.paymentstop.PaymStopTypeClass;
import com.temenos.t24.api.records.paymentstop.PaymentStopRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.tafj.api.client.impl.T24Context;

/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */
public class EffetChequeStopCharge extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        
        PaymentStopRecord paymentRec = new PaymentStopRecord(currentRecord);
        List<PaymStopTypeClass> paymstoptype = paymentRec.getPaymStopType();
        DataAccess da = new DataAccess((T24Context)this);
        FtCommissionTypeRecord ComRec1 = new FtCommissionTypeRecord(da.getRecord("CIV", "FT.COMMISSION.TYPE", "", "EFFOPP"));
        String ComAmt1 = ComRec1.getCurrency(0).getFlatAmt().getValue();
        
        for (int i = 0; i < paymstoptype.size(); i++) {
            try {
                PaymStopTypeClass stoptype = paymentRec.getPaymStopType(i);
                List<ChargeCodeClass> chargeCodeList = stoptype.getChargeCode();
                if (chargeCodeList == null || chargeCodeList.isEmpty()) {
                        ChargeCodeClass chargecode = new ChargeCodeClass();
                        chargecode.setChargeCode("EFFOPP");
                        chargecode.setChgAmount(ComAmt1);
                        stoptype.setChargeCode(chargecode, i);
                } else
                {
                    ChargeCodeClass chargecode;
                    if (i < chargeCodeList.size()) {
                        chargecode = chargeCodeList.get(i);
                    } else {
                        chargecode = new ChargeCodeClass();
                    }
                    chargecode.setChargeCode("EFFOPP");
                    chargecode.setChgAmount(ComAmt1);
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
