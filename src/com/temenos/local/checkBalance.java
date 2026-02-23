package com.temenos.local;

import java.util.List;

import com.temenos.api.TNumber;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.party.Account;
import com.temenos.t24.api.complex.ac.accountapi.*;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.presyndicationfile.DevolUwClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.tafj.api.client.impl.T24Context;
/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */
public class checkBalance extends Enquiry {

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        // TODO Auto-generated method stub
        
        
        String txnref = value;
        DataAccess da = new DataAccess(this);
        
        AccountRecord Accrec = new AccountRecord(da.getRecord("ACCOUNT",txnref));
        Double online = Double.parseDouble(Accrec.getOnlineClearedBal().toString());
        Double working = Double.parseDouble(Accrec.getWorkingBalance().toString());
        
        Double net = online - working;
        
        System.out.println(Accrec.toString());
        Account Ac = new Account(this);
        String currencyId = Accrec.getCurrency().toString();
        Ac.setAccountId(txnref);
        System.out.println(currencyId);
//        String AvBal = Ac.getAvailableAmount(currencyId).toString();
//        Amount bal = Ac.getAvailableAmount(currencyId);
        TNumber avbal = Ac.getAvailableBalance().getAmount().getValue();
        
        Double davbal = Double.parseDouble(avbal.toString());
        
        Double finbal = davbal + net;
        String formattedValue = String.format("%.2f", finbal);
        
        System.out.println(formattedValue.toString());
        return formattedValue.toString();
    }

}
