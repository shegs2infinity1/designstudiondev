package com.mcbc.bbg.sn.teller;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.LegalIdClass;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebcommonparambbgsn.EbCommonParamBbgSnRecord;
import com.temenos.t24.api.tables.ebcommonparambbgsn.ParamNameClass;
import com.temenos.tafj.api.client.impl.T24Context;
import java.util.ArrayList;
import java.util.List;

public class DepositTransaction extends RecordLifecycle {
  DataAccess da = new DataAccess((T24Context)this);
  
  boolean AccountDormant;
  
  String txnCode;
  
  EbCommonParamBbgSnRecord bbgComParamRec = new EbCommonParamBbgSnRecord();
  
  boolean debugg = true;
  
  public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord, TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
    String txnConductor = "";
    String customerId = "";
    TellerRecord tellerRecord = new TellerRecord(currentRecord);
    txnConductor = tellerRecord.getLocalRefField("L.DEP.CONDUCTOR").getValue().toString();
    if (txnConductor.equals("Titulaire du Compte")) {
      tellerRecord.getLocalRefField("L.TP.FIRST.NAME").setValue("");
      tellerRecord.getLocalRefField("L.TP.LAST.NAME").setValue("");
      tellerRecord.getLocalRefField("L.TP.ADDRESS").setValue("");
      tellerRecord.getLocalRefField("L.TP.COUNTRY").setValue("");
      tellerRecord.getLocalRefField("L.TP.LEGAL.ID").setValue("");
      tellerRecord.getLocalRefField("L.TP.ID.ISS.DT").setValue("");
      tellerRecord.getLocalRefField("L.TP.ID.EXP.DT").setValue("");
      tellerRecord.getLocalRefField("L.TP.OCCUPATION").setValue("");
      tellerRecord.getLocalRefField("L.TP.TELEPHONE").setValue("");
      tellerRecord.getLocalRefField("L.DATE.OF.BIRTH").setValue("");
      tellerRecord.getLocalRefField("L.TP.LEGAL.DOC.NAME").setValue("");
//      tellerRecord.getLocalRefField("L.TP.CUST.BIRTH.CITY").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.LEGAL.DOC.NAME").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.OCCUP").setValue("");
      tellerRecord.getLocalRefField("L.CUST.BIRTH").setValue("");
      customerId = tellerRecord.getCustomer1().toString().equals("") ? tellerRecord.getCustomer2().toString() : 
        tellerRecord.getCustomer1().toString();
      DataAccess da = new DataAccess((T24Context)this);
      CustomerRecord customerRecord = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
      tellerRecord.getLocalRefField("L.TP.COUNTRY").setValue(customerRecord.getResidence().toString());
      try {
        tellerRecord.getLocalRefField("L.TP.FIRST.NAME").setValue(customerRecord.getGivenNames().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      
      try {
        tellerRecord.getLocalRefField("L.TP.LAST.NAME").setValue(customerRecord.getFamilyName().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.ADDRESS")
          .setValue(customerRecord.getStreet(0).getValue().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.LEGAL.ID")
          .setValue(customerRecord.getLegalId(0).getLegalId().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.ID.ISS.DT")
          .setValue(customerRecord.getLegalId(0).getLegalIssDate().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.ID.EXP.DT")
          .setValue(customerRecord.getLegalId(0).getLegalExpDate().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.OCCUPATION")
          .setValue(customerRecord.getEmploymentStatus(0).getOccupation().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.TELEPHONE")
          .setValue(customerRecord.getPhone1(0).getSms1().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.DATE.OF.BIRTH")
          .setValue(customerRecord.getDateOfBirth().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.LEGAL.DOC.NAME")
          .setValue(((LegalIdClass)customerRecord.getLegalId().get(0)).getLegalDocName().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
//      try {
//        tellerRecord.getLocalRefField("L.TP.CUST.BIRTH.CITY")
//          .setValue(customerRecord.getCustBirthCity().toString());
//      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.LEGAL.DOC.NAME")
          .setValue(customerRecord.getLegalIdDocName().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY")
          .setValue(customerRecord.getCustBirthCity().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.OCCUP")
          .setValue(customerRecord.getEmploymentStatus(0).getOccupation().getValue());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          tellerRecord.getLocalRefField("L.CUST.BIRTH").setValue(customerRecord.getCustBirthCountry().toString());
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
    } 
    currentRecord.set(tellerRecord.toStructure());
  }
  
  public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord, TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
    String commonParamId = "TELLER.WITHDRAWAL";
    List<String> ParamList = new ArrayList<>();
    TellerRecord tellerRecord = new TellerRecord(currentRecord);
    TField depositorConductor = tellerRecord.getLocalRefField("L.TP.COUNTRY");
    TField depositorCountry = tellerRecord.getLocalRefField("L.TP.COUNTRY");
    TField depositorName = tellerRecord.getLocalRefField("L.TP.FIRST.NAME");
    TField depositorLName = tellerRecord.getLocalRefField("L.TP.LAST.NAME");
    TField depositorAddress = tellerRecord.getLocalRefField("L.TP.ADDRESS");
    TField depositorDate = tellerRecord.getLocalRefField("L.DATE.OF.BIRTH");
    TField depositorLegalDoc = tellerRecord.getLocalRefField("L.TP.LEGAL.DOC.NAME");
//    TField depositorCustBirth = tellerRecord.getLocalRefField("L.TP.CUST.BIRTH.CITY");
    TField depositorOccupation = tellerRecord.getLocalRefField("L.TP.OCCUPATION");
    TField depositorLegalId = tellerRecord.getLocalRefField("L.TP.LEGAL.ID");
    TField depositorLegalIssueDate = tellerRecord.getLocalRefField("L.TP.ID.ISS.DT");
    TField depositorLegalExpiryDate = tellerRecord.getLocalRefField("L.TP.ID.EXP.DT");
    TField depositorTelephone = tellerRecord.getLocalRefField("L.TP.TELEPHONE");
    TField depositorLCustBirth = tellerRecord.getLocalRefField("L.CUST.BIRTH");
    String txnCode = tellerRecord.getTransactionCode().toString();
    if (this.debugg)
      System.out.println("Entering Routine "); 
    this.bbgComParamRec = new EbCommonParamBbgSnRecord(this.da.getRecord("EB.COMMON.PARAM.BBG.SN", commonParamId));
    List<ParamNameClass> pnclassList = this.bbgComParamRec.getParamName();
    List<TField> txnCodeParamList = new ArrayList<>();
    for (ParamNameClass pnclass : pnclassList) {
      String ParamName = pnclass.getParamName().toString();
      if (this.debugg)
        System.out.println("ParamName got " + ParamName); 
      List<TField> PvalueList = pnclass.getParamValue();
      if (ParamName.equals("TXN.CODE"))
        txnCodeParamList = PvalueList; 
    } 
    for (TField ParmTxnid : txnCodeParamList) {
      if (this.debugg)
        System.out.println("Checking txn Param " + ParmTxnid); 
      ParamList.add(ParmTxnid.toString());
    } 
    String Acct2 = tellerRecord.getAccount2().toString();
    AccountRecord accRec = new AccountRecord(this.da.getRecord("ACCOUNT", Acct2));
    String arrangementId = accRec.getArrangementId().toString();
    System.out.println("Arrangement Id " + arrangementId);
    if (!(arrangementId.length() == 0))
    {
           
          try {
              AaAccountDetailsRecord aaAccRecord = new AaAccountDetailsRecord(this.da.getRecord("AA.ACCOUNT.DETAILS", arrangementId));
              String DormancyStatus = aaAccRecord.getArrDormancyStatus().toString();
              System.out.println("DormancyStatus " + DormancyStatus);
              if (DormancyStatus.length() == 0) {
                  this.AccountDormant = false;
              } else {
                  this.AccountDormant = true;
              } 
        } catch (Exception e) {
           System.out.println(e);
        }
         
          
     }else {
         if (!(Acct2.length()==16)){
             tellerRecord.getAccount2().setError("Not an Arrangement");
         }
    }
    System.out.println("txnCode before Checking " + txnCode);
    if (ParamList.contains(txnCode))
      if (this.AccountDormant) {
        tellerRecord.getAccount2().setError("Inactif/Dormant");
        System.out.println("Account is Dormant raising error ");
        tellerRecord.getAccount2().setError("Account Inactif");
      }  
    if (depositorConductor.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorConductor " + depositorConductor.getValue().toString());
      depositorConductor.setError("Champ obligatoire.");
      }
    if (depositorDate.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorDate " + depositorDate.getValue().toString());
      depositorDate.setError("Champ obligatoire."); 
      }
    if (depositorLegalDoc.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorLegalDoc " + depositorLegalDoc.getValue().toString());
      depositorLegalDoc.setError("Champ obligatoire.");
      }
//    if (depositorCustBirth.getValue().toString().trim().length() == 0)
//    {
//        System.out.println("depositorCustBirth " + depositorCustBirth.getValue().toString());
//      depositorCustBirth.setError("Champ obligatoire.");
//      }
    if (depositorCountry.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorCountry " + depositorCountry.getValue().toString());
      depositorCountry.setError("Champ obligatoire."); 
      }
    if (depositorName.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorName " + depositorName.getValue().toString());
      depositorName.setError("Champ obligatoire."); 
      }
    if (depositorLName.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorLName " + depositorLName.getValue().toString());
      depositorLName.setError("Champ obligatoire.");
      }
    if (depositorAddress.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorAddress " + depositorAddress.getValue().toString());
      depositorAddress.setError("Champ obligatoire.");
      }
    if (depositorLegalId.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorLegalId " + depositorLegalId.getValue().toString());
      depositorLegalId.setError("Champ obligatoire.");
      }
    if (depositorLegalIssueDate.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorLegalIssueDate " + depositorLegalIssueDate.getValue().toString());
      depositorLegalIssueDate.setError("Champ obligatoire.");
      }
    if (depositorLegalExpiryDate.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorLegalExpiryDate " + depositorLegalExpiryDate.getValue().toString());
      depositorLegalExpiryDate.setError("Champ obligatoire.");
      }
    if (depositorTelephone.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorTelephone " + depositorTelephone.getValue().toString());
      depositorTelephone.setError("Champ obligatoire."); 
      }
    if (depositorOccupation.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorOccupation " + depositorOccupation.getValue().toString());
      depositorOccupation.setError("Champ obligatoire."); 
      }
    if (depositorLCustBirth.getValue().toString().trim().length() == 0)
    {
        System.out.println("depositorLCustBirth " + depositorLCustBirth.getValue().toString());
        depositorLCustBirth.setError("Champ obligatoire.");
        }
    return tellerRecord.getValidationResponse();
  }
}
