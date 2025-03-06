package com.mcbc.bbg.sn.teller;

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

public class ChequeTransaction extends RecordLifecycle {
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
      tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY").setValue("");
      tellerRecord.getLocalRefField("L.BEAR.OCCUP").setValue("");
      customerId = tellerRecord.getCustomer1().toString().equals("") ? tellerRecord.getCustomer2().toString() : tellerRecord.getCustomer1().toString();
      DataAccess da = new DataAccess((T24Context)this);
      CustomerRecord customerRecord = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
      try {
        tellerRecord.getLocalRefField("L.BEAR.FNAME").setValue(customerRecord.getShortName(0).toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.LNAME").setValue(customerRecord.getFamilyName().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.ADDRESS").setValue(customerRecord.getStreet(0).getValue().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.LEG.ID").setValue(customerRecord.getLegalId(0).getLegalId().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.LEG.IS").setValue(customerRecord.getLegalId(0).getLegalIssDate().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.LEG.EX").setValue(customerRecord.getLegalId(0).getLegalExpDate().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.TEL.NO").setValue(customerRecord.getPhone1(0).getPhone1().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.DATE.OF.BIRTH").setValue(customerRecord.getDateOfBirth().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.TP.LEGAL.DOC.NAME").setValue(((LegalIdClass)customerRecord.getLegalId().get(0)).getLegalDocName().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.CUST.BIRTH").setValue(customerRecord.getCustBirthCity().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.LEGAL.DOC.NAME").setValue(((LegalIdClass)customerRecord.getLegalId().get(0)).getLegalDocName().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY").setValue(customerRecord.getCustBirthCity().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
      try {
        tellerRecord.getLocalRefField("L.BEAR.OCCUP").setValue(customerRecord.getEmploymentStatus(0).getOccupation().toString());
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {}
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
    TField bearerCustBirthCity = tellerRecord.getLocalRefField("L.BEAR.CUST.BIRTH.CITY");
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
