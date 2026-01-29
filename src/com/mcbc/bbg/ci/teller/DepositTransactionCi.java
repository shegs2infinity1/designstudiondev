package com.mcbc.bbg.ci.teller;

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

public class DepositTransactionCi extends RecordLifecycle {
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
      try{
          String Residence = customerRecord.getResidence().toString();
          if (Residence.isEmpty()){
              Residence = "CI";
          }
          tellerRecord.getLocalRefField("L.TP.COUNTRY").setValue(Residence);
      }catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String GivenNames = customerRecord.getGivenNames().toString();
          if (GivenNames.isEmpty()){
              GivenNames = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.TP.FIRST.NAME").setValue(GivenNames);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      
      try {
          String FamilyName = customerRecord.getFamilyName().toString();
          if (FamilyName.isEmpty()){
              FamilyName = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.TP.LAST.NAME").setValue(FamilyName);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String Street = customerRecord.getStreet(0).getValue().toString();
          if (Street.isEmpty()){
              Street =  "Non Defini";
          }
        tellerRecord.getLocalRefField("L.TP.ADDRESS").setValue(Street);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String LegalId = customerRecord.getLegalId(0).getLegalId().toString();
          if (LegalId.isEmpty()){
              LegalId = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.TP.LEGAL.ID").setValue(LegalId);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String LegalIssDate = customerRecord.getLegalId(0).getLegalIssDate().toString();
          if (LegalIssDate.isEmpty()){
              LegalIssDate = "19990101";
          }
        tellerRecord.getLocalRefField("L.TP.ID.ISS.DT").setValue(LegalIssDate);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String LegalExpDate = customerRecord.getLegalId(0).getLegalExpDate().toString();
          if (LegalExpDate.isEmpty()){
              LegalExpDate = "19990101";
          }
        tellerRecord.getLocalRefField("L.TP.ID.EXP.DT").setValue(LegalExpDate);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      System.out.println("Checking Occupation");
      try {
          String TpOccupation = customerRecord.getEmploymentStatus(0).getOccupation().toString();
          System.out.println("Checking Occupation"+ TpOccupation+" lenght is "+ TpOccupation.trim().length());
          if (TpOccupation.trim().length() == 0){
              TpOccupation = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.TP.OCCUPATION").setValue(TpOccupation);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String Sms1 = customerRecord.getPhone1(0).getSms1().toString();
          if (Sms1.isEmpty()){
              Sms1 = "0123456789012";
          }
        tellerRecord.getLocalRefField("L.TP.TELEPHONE").setValue(Sms1);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String DateOfBirth = customerRecord.getDateOfBirth().toString();
          if (DateOfBirth.isEmpty()){
              DateOfBirth = "19990101";
          }
        tellerRecord.getLocalRefField("L.DATE.OF.BIRTH").setValue(DateOfBirth);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String LegalDocName = ((LegalIdClass)customerRecord.getLegalId().get(0)).getLegalDocName().toString();
          if (LegalDocName.isEmpty()){
              LegalDocName = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.TP.LEGAL.DOC.NAME").setValue(LegalDocName);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String LegalIdDocName = customerRecord.getLegalIdDocName().toString();
          if (LegalIdDocName.isEmpty()){
              LegalIdDocName = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.BEAR.LEGAL.DOC.NAME").setValue(LegalIdDocName);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String CustBirthCity = customerRecord.getCustBirthCity().toString();
          if (CustBirthCity.isEmpty()){
              CustBirthCity = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY").setValue(CustBirthCity);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String Occupation = customerRecord.getEmploymentStatus(0).getOccupation().getValue();
          System.out.println("Checking Occupation"+ Occupation+" lenght is "+ Occupation.trim().length());
          if (Occupation.isEmpty()){
              Occupation = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.BEAR.OCCUP").setValue(Occupation);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String CustBirthCountry = customerRecord.getCustBirthCountry().toString();
          if (CustBirthCountry.isEmpty()){
              CustBirthCountry = "Non Defini";
          }
          tellerRecord.getLocalRefField("L.CUST.BIRTH").setValue(CustBirthCountry);
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
    } 
    
    TField bearerOccupation = tellerRecord.getLocalRefField("L.TP.OCCUPATION");
    if (bearerOccupation.getValue().toString().trim().length() == 0){
        tellerRecord.getLocalRefField("L.TP.OCCUPATION").setValue("Non Defini");
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
