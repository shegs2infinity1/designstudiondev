package com.mcbc.tmb.cu;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author Debesh * /** * @category EB.API - V.VAL.CUS.LICENSE.TMB
 * 
 * @param Application
 *            - CUSTOMER
 * @param INPUT.ROUTINE
 *            - Raise error if the L.ETVA.TMB or L.ERSC.TMB is NULL WHEN L.TVA &
 *            L.RSC is NON
 * 
 *            - CUSTOMER,INPUT.IND.TMB - CUSTOMER,CORP.NIND.TMB -
 * @return none
 *
 */
public class tmbCheckLicense extends RecordLifecycle {

    DataAccess da = new DataAccess(this);
    CustomerRecord custRec;
    String letva;
    String lersc;
    String ltva;
    String lrsc;
    String CustomerId;

    boolean debugg = true;
    TField tfletva;
    TField tflersc;
    String OvrValue;
    public String TargetId;
    TField FieldRSC;
    TField FieldTVA;
    String rscFieldName;
    String tvaFieldName;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub

        if (debugg)
            System.out.println("Entered Input routine ");

        try {
            custRec = new CustomerRecord(currentRecord);
            CustomerId = currentRecordId;
            TargetId = custRec.getTarget().toString();
            TargetId = TargetId.trim();
            int intTarget = Integer.parseInt(TargetId);
            if (debugg)
                System.out.println("Target id " + TargetId);

            tfletva = custRec.getLocalRefField("L.ETVA.TMB");
            tflersc = custRec.getLocalRefField("L.ERSC.TMB");

            letva = custRec.getLocalRefField("L.ETVA.TMB").getValue();
            lersc = custRec.getLocalRefField("L.ERSC.TMB").getValue();
            ltva = custRec.getLocalRefField("L.TVA").getValue();
            lrsc = custRec.getLocalRefField("L.RSC").getValue();
            if (debugg) {
                System.out.println("letva " + letva);
                System.out.println("lersc " + lersc);
            }

    /*        if ((intTarget == 52) || (intTarget == 53)) {
                if (lrsc.equals("OUI")) {
                    rscFieldName = "L.RSC";
                    RaiseOveridde();
                }
                if (ltva.equals("OUI")) {
                    rscFieldName = "L.TVA";
                    RaiseOveridde();
                }

            } else {
                if (lrsc.equals("NON")) {
                    rscFieldName = "L.RSC";
                    RaiseOveridde();
                }

                if (ltva.equals("NON")) {
                    rscFieldName = "L.TVA";
                    RaiseOveridde();
                }

            }
*/            
            

        } catch (Exception e) {
            System.out.println("Exception input routinE reading LR " + e);
        }

        try {
            if ((letva.length() == 0) && ltva.equals("NON")) {
                if (debugg)
                 System.out.println("setting error eltva " + letva);
                List<String> Errltva = new ArrayList<>();
                String ErrValue = "CL-LETVA.MANDATORY";
                Errltva.add(ErrValue);
                tfletva.setError(Errltva.toString());
            }
            if (lersc.length() == 0 && lrsc.equals("NON")) {
                if (debugg)
                    System.out.println("setting error lersc " + lersc);
                List<String> Errltrsc = new ArrayList<>();
                String ErrValue = "CL-LERSC.MANDATORY";
                Errltrsc.add(ErrValue);
                tflersc.setError(Errltrsc.toString());
            }

        } catch (Exception e) {
            System.out.println("Exception input routine setting error " + e);
        }
        //custRec.setReserved51("");
        if (debugg)
            System.out.println("Reseved flag unset");
        currentRecord.set(custRec.toStructure());
        return custRec.getValidationResponse();
    }

    public void RaiseOveridde() {
        System.out.println("Changes found in  " + rscFieldName);
        List<String> Ovrlrsc = new ArrayList<>();
        OvrValue = "CG-CUSTOMER.NON.TAX.TMB";
        Ovrlrsc.add(OvrValue);
        Ovrlrsc.add(CustomerId);
        Ovrlrsc.add(rscFieldName);
        if (rscFieldName.equals("L.RSC")) {
            FieldRSC.setOverride(Ovrlrsc.toString());
        } else {
            FieldTVA.setOverride(Ovrlrsc.toString());
        }
    }

}
