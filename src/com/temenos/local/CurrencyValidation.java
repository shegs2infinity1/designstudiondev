package com.temenos.local;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.*;
import com.temenos.t24.api.records.fundstransfer.*;

/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */
public class CurrencyValidation extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        
        FundsTransferRecord ftrecord = new FundsTransferRecord(currentRecord);
        TField debitccy = ftrecord.getDebitCurrency();
        TField creditccy = ftrecord.getCreditCurrency();
        if (!debitccy.getValue().equals(creditccy.getValue())){
            List<String> param = new ArrayList<>();
            param.add("Error in Currency");
            param.add("1");
            param.add(creditccy.toString());
            param.add(debitccy.toString());
            
            creditccy.setOverride(param.toString());
            
        }
        
        return ftrecord.getValidationResponse();
//        return super.validateRecord(application, currentRecordId, currentRecord, unauthorisedRecord, liveRecord,
//                transactionContext);
    }

    
}
