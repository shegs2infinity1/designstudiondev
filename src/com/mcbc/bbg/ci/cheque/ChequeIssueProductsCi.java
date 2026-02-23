package com.mcbc.bbg.ci.cheque;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.alternateaccount.AlternateAccountRecord;
import com.temenos.t24.api.records.chequetype.ChequeTypeRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.tafj.api.client.impl.T24Context;
import java.util.List;

public class ChequeIssueProductsCi extends Enquiry {
  public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
    FilterCriteria newFilterCriteria = new FilterCriteria();
    FilterCriteria cusFilterCriteria = new FilterCriteria();
    String accountParam = ((FilterCriteria)filterCriteria.get(0)).getFieldname();
    String accountParamValue = ((FilterCriteria)filterCriteria.get(0)).getValue();
    newFilterCriteria.setFieldname(accountParam);
    newFilterCriteria.setOperand("NE");
    newFilterCriteria.setValue(accountParamValue);
    cusFilterCriteria.setFieldname("CHEQUE.TYPE");
    cusFilterCriteria.setOperand("EQ");
    DataAccess da = new DataAccess((T24Context)this);
    
    System.out.println("Account Number "+ accountParamValue);
    
    AccountRecord accountRec = new AccountRecord();
    AlternateAccountRecord altRec = new AlternateAccountRecord();
    try {
        System.out.println("Account Number Check" + accountParamValue.length());
        if (accountParamValue.length() == 12){
            accountRec = new AccountRecord(da.getRecord("ACCOUNT", accountParamValue));
        } else if (accountParamValue.length() == 11) {
            System.out.println("Alter Account Number Check "+ accountParamValue.length());
            altRec = new AlternateAccountRecord(da.getRecord("ALTERNATE.ACCOUNT", accountParamValue));
            System.out.println("Alter Account Number "+ altRec);
            String acctnum = altRec.getGlobusAcctNumber().getValue();
            accountParamValue = acctnum ;
            accountRec = new AccountRecord(da.getRecord("ACCOUNT", accountParamValue));
        } else {
            System.out.println("Invalid Account Number");
        }
    } catch (Exception e) {
        System.out.println(e);
    }
    
    String customerNo = accountRec.getCustomer().getValue();
    String acctCategory = accountRec.getCategory().getValue();
    String acctCoCode = accountRec.getCoCode();
    String AcctCountry = acctCoCode.substring(0, 2);
    StringBuilder chequeTypeParam = new StringBuilder("");
    if (!customerNo.isEmpty()) {
      CustomerRecord customerRec = new CustomerRecord(da.getRecord("CUSTOMER", customerNo));
      String customerTarget = customerRec.getTarget().toString().trim();
      System.out.println("Before Read Cheque Type");
      List<String> chequeTypelist = da.selectRecords("", "CHEQUE.TYPE", "", "WITH CATEGORY EQ " + acctCategory+" AND CO.CODE LIKE ..."+ AcctCountry +"...");
      System.out.println("After Read Cheque Type" + chequeTypelist.toString());
      if (!chequeTypelist.isEmpty()) {
        for (String currentChequeType : chequeTypelist) {
          System.out.println("Current Cheque Type -> " + currentChequeType);
          System.out.println("Current Customer Target -> " + customerTarget);
          ChequeTypeRecord chequeTypeRec = new ChequeTypeRecord(da.getRecord("CHEQUE.TYPE", currentChequeType));
          System.out.println("Allow Target Values Size -> " + chequeTypeRec.getLocalRefGroups("L.ALLOW.TARGET").size());
          System.out.println("Allow Target Values -> " + chequeTypeRec.getLocalRefGroups("L.ALLOW.TARGET").toArray().toString());
          for (int counter = 0; chequeTypeRec.getLocalRefGroups("L.ALLOW.TARGET").size() > counter; counter++) {
            System.out.println("List Item->" + chequeTypeRec.getLocalRefGroups("L.ALLOW.TARGET").get(counter).getLocalRefField("L.ALLOW.TARGET").getValue());
            if (chequeTypeRec.getLocalRefGroups("L.ALLOW.TARGET").get(counter).getLocalRefField("L.ALLOW.TARGET").toString().trim().equals(customerTarget)) {
              System.out.println("Target Found-> " + customerTarget);
              chequeTypeParam.append(currentChequeType).append(" ");
            } 
          } 
        } 
        System.out.println("cheque Type Param -> " + chequeTypeParam.toString());
      } 
    } 
    filterCriteria.clear();
    filterCriteria.add(newFilterCriteria);
    cusFilterCriteria.setValue(chequeTypeParam.toString());
    filterCriteria.add(cusFilterCriteria);
    return filterCriteria;
  }
  
  public String setValue(String value, String currentId, TStructure currentRecord, List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
    String accountParamValue = ((FilterCriteria)filterCriteria.get(0)).getValue();
    return accountParamValue;
  }
}
