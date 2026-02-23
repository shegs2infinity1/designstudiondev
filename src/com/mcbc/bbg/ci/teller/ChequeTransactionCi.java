package com.mcbc.bbg.ci.teller;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.LegalIdClass;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.tafj.api.client.impl.T24Context;

public class ChequeTransactionCi extends RecordLifecycle {
  public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord, TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
    String txnConductor = "";
    String customerId = "";
    TellerRecord tellerRecord = new TellerRecord(currentRecord);
    txnConductor = tellerRecord.getLocalRefField("L.CHQ.CONDUCTOR").getValue().toString();
    if (txnConductor.equals("Titulaire du Compte")) {
      tellerRecord.getLocalRefField("L.BEAR.FNAME").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.LNAME").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.ADDRESS").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.LEG.ID").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.LEG.IS").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.LEG.EX").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.TEL.NO").setValue("");
//      tellerRecord.getLocalRefField("L.TT.D.O.B").setValue("");
      tellerRecord.getLocalRefField("L.DATE.OF.BIRTH").setValue("");
      tellerRecord.getLocalRefField("L.TP.LEGAL.DOC.NAME").setValue("");
//      tellerRecord.getLocalRefField("L.TP.CUST.BIRTH.CITY").setValue("");
      tellerRecord.getLocalRefField("L.CUST.BIRTH").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.LEGAL.DOC.NAME").setValue("");
//      tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.OCCUP").setValue("");
      customerId = tellerRecord.getCustomer1().toString().equals("") ? tellerRecord.getCustomer2().toString() : tellerRecord.getCustomer1().toString();
      DataAccess da = new DataAccess((T24Context)this);
      CustomerRecord customerRecord = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
      try {
        String bname = customerRecord.getShortName(0).toString();
        if (bname.isEmpty()){
            bname = "Non Defini";
        }
        tellerRecord.getLocalRefField("L.BEAR.FNAME").setValue(bname);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        String lname = customerRecord.getFamilyName().toString();
        if (lname.isEmpty()){
            lname = "Non Defini";
        }
        tellerRecord.getLocalRefField("L.BEAR.LNAME").setValue(lname);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String ladress = customerRecord.getStreet(0).getValue().toString();
          if (ladress.isEmpty()){
              ladress = "Non Defini";
          }  
        tellerRecord.getLocalRefField("L.BEAR.ADDRESS").setValue(ladress);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String legid = customerRecord.getLegalId(0).getLegalId().toString();
          if (legid.isEmpty()){
              legid = "Non Defini";
          }  
        tellerRecord.getLocalRefField("L.BEAR.LEG.ID").setValue(legid);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String legisdate = customerRecord.getLegalId(0).getLegalIssDate().toString();
          if (legisdate.isEmpty()){
              legisdate = "19990101";
          } 
        tellerRecord.getLocalRefField("L.BEAR.LEG.IS").setValue(legisdate);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String LegalExpDate = customerRecord.getLegalId(0).getLegalExpDate().toString();
          if (LegalExpDate.isEmpty()){
              LegalExpDate = "19990101";
          }
        tellerRecord.getLocalRefField("L.BEAR.LEG.EX").setValue(LegalExpDate);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String Phone1 = customerRecord.getPhone1(0).getPhone1().toString();
          if (Phone1.isEmpty()){
              Phone1 = "Non Defini";
          }
        tellerRecord.getLocalRefField("L.BEAR.TEL.NO").setValue(Phone1);
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
          String CustBirthCity = customerRecord.getCustBirthCity().toString();
          if (CustBirthCity.isEmpty()){
              CustBirthCity = "Non Defini";
          }  
        tellerRecord.getLocalRefField("L.CUST.BIRTH").setValue(CustBirthCity);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String LegalDocName = ((LegalIdClass)customerRecord.getLegalId().get(0)).getLegalDocName().toString();
          if (LegalDocName.isEmpty()){
              LegalDocName = "Non Defini";
          }  
        tellerRecord.getLocalRefField("L.BEAR.LEGAL.DOC.NAME").setValue(LegalDocName);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
//      try {
//          String CustBirthCity = customerRecord.getCustBirthCity().toString();
//          if (CustBirthCity.isEmpty()){
//              CustBirthCity = "Non Defini";
//          }  
//        tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY").setValue(CustBirthCity);
//      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
          String Occupation = customerRecord.getEmploymentStatus(0).getOccupation().toString();
          System.out.println("Checking Occupation"+ Occupation+" lenght is "+ Occupation.trim().length());
          if (Occupation.trim().length() == 0){
              Occupation = "Non Defini";
          }  
        tellerRecord.getLocalRefField("L.BEAR.OCCUP").setValue(Occupation);
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
    } 
    TField bearerOccupation = tellerRecord.getLocalRefField("L.BEAR.OCCUP");
    if (bearerOccupation.getValue().toString().trim().length() == 0){
        tellerRecord.getLocalRefField("L.BEAR.OCCUP").setValue("Non Defini");
    }
    currentRecord.set(tellerRecord.toStructure());
  }
  
  public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord, TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
    String txnConductor = "";
    TellerRecord tellerRecord = new TellerRecord(currentRecord);
    txnConductor = tellerRecord.getLocalRefField("L.CHQ.CONDUCTOR").getValue().toString();
    TField bearerName = tellerRecord.getLocalRefField("L.BEAR.FNAME");
    TField bearerLName = tellerRecord.getLocalRefField("L.BEAR.LNAME");
    TField bearerAddress = tellerRecord.getLocalRefField("L.BEAR.ADDRESS");
//    TField depositorDate = tellerRecord.getLocalRefField("L.TT.D.O.B");
    TField depositorDate = tellerRecord.getLocalRefField("L.DATE.OF.BIRTH");
    TField bearerLegalId = tellerRecord.getLocalRefField("L.BEAR.LEG.ID");
    TField bearerLegalIdIssueDate = tellerRecord.getLocalRefField("L.BEAR.LEG.IS");
    TField bearerLegalIdExpiryDate = tellerRecord.getLocalRefField("L.BEAR.LEG.EX");
    TField bearerLegalDocName = tellerRecord.getLocalRefField("L.BEAR.LEGAL.DOC.NAME");
//    TField bearerCustBirthCity = tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY");
    TField bearerCustBirthCity = tellerRecord.getLocalRefField("L.CUST.BIRTH");
    TField bearerTelephone = tellerRecord.getLocalRefField("L.BEAR.TEL.NO");
    TField bearerOccupation = tellerRecord.getLocalRefField("L.BEAR.OCCUP");
    if (txnConductor.contains("Porteur")) {
      if (bearerName.getValue().toString().trim().length() == 0)
        bearerName.setError("Champ obligatoire."); 
      if (depositorDate.getValue().toString().trim().length() == 0)
        depositorDate.setError("Champ obligatoire."); 
      if (bearerOccupation.getValue().toString().trim().length() == 0)
        bearerOccupation.setError("Champ obligatoire."); 
      if (bearerCustBirthCity.getValue().toString().trim().length() == 0)
        bearerCustBirthCity.setError("Champ obligatoire."); 
      if (bearerLegalDocName.getValue().toString().trim().length() == 0)
        bearerLegalDocName.setError("Champ obligatoire."); 
      if (bearerLName.getValue().toString().trim().length() == 0)
        bearerLName.setError("Champ obligatoire."); 
      if (bearerAddress.getValue().toString().trim().length() == 0)
        bearerAddress.setError("Champ obligatoire."); 
      if (bearerLegalId.getValue().toString().trim().length() == 0)
        bearerLegalId.setError("Champ obligatoire."); 
      if (bearerLegalIdIssueDate.getValue().toString().trim().length() == 0)
        bearerLegalIdIssueDate.setError("Champ obligatoire."); 
      if (bearerLegalIdExpiryDate.getValue().toString().trim().length() == 0)
        bearerLegalIdExpiryDate.setError("Champ obligatoire."); 
      if (bearerTelephone.getValue().toString().trim().length() == 0)
        bearerTelephone.setError("Champ obligatoire."); 
    } 
    return tellerRecord.getValidationResponse();
  }
}
