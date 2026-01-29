package com.mcbc.tmb.pymt;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;


/**
 * TODO: Document me!
 *
 *
 * * /** * @category EB.API - V.VAL.CONDUCTOR.TMB
 * 
 * @param Application
 *            - CUSTOMER
 * @param INPUT Routine
 *            - For Conductor third Party deposits , check for mandatory input in local fields
 * @param Version
 *            - TELLER,LCY.OPDEP.E13.TMB 
 *            CSD_Payment_Validation2.jar
 * @return none
 *
 */

public class tmbCheckConductor extends RecordLifecycle {

    DataAccess da = new DataAccess(this);
    TellerRecord tellerRec;
    boolean debugg = false;
    String lThirdParty = "";
    String lNameOther = "";
    String lIdType = "";
    String lIdOther = "";
    String lAddOther = "";
    String lNumOther = "";

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        try {
            tellerRec = new TellerRecord(currentRecord);

            lThirdParty = tellerRec.getLocalRefField("L.THIRD.PARTY").toString();
            lNameOther = tellerRec.getLocalRefField("L.NAME.OTH").toString();
            lIdType = tellerRec.getLocalRefField("L.ID.TYPE").toString();
            lIdOther = tellerRec.getLocalRefField("L.ID.OTH").toString();
            lAddOther = tellerRec.getLocalRefField("L.ADD.OTH").toString();
            lNumOther = tellerRec.getLocalRefField("L.NUM.OTH").toString();
            
            
            if (lThirdParty.length() > 0) {
              
            if (lNameOther.length() == 0)
                tellerRec.getLocalRefField("L.NAME.OTH").setError("Mandatory Field");
            if (lIdType.length() == 0)
                tellerRec.getLocalRefField("L.ID.TYPE").setError("Mandatory Field");
            if (lIdOther.length() == 0)
                tellerRec.getLocalRefField("L.ID.OTH").setError("Mandatory Field");
            if (lAddOther.length() == 0)
                tellerRec.getLocalRefField("L.ADD.OTH").setError("Mandatory Field");
            if (lNumOther.length() == 0)
                tellerRec.getLocalRefField("L.NUM.OTH").setError("Mandatory Field");
            }
            
        } catch (Exception e) {
            System.out.println(" Error " + e);
        }

        currentRecord.set(tellerRec.toStructure());
        return tellerRec.getValidationResponse();
    }

}
