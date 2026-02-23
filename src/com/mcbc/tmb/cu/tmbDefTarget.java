package com.mcbc.tmb.cu;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 *
 * * /** * @category EB.API - V.DEFA.CUST.TARGET.TMB
 * 
 * @param Application
 *            - CUSTOMER
 * @param VALIDATION.ROUTINE
 *            - Check if the TARGET is 'ambassador' (hard coded for now but
 *            later to use EB.COMMON.PARAM)
 * @param Version
 *            - CUSTOMER,INPUT.CU10.MCBC
 * @return none
 *
 */
public class tmbDefTarget extends RecordLifecycle {

    public String TargetId;
    public Boolean debugg = true;
    DataAccess DataObj = new DataAccess(this);
    CustomerRecord custRec;
    String ltva = "";
    String lrsc = "";
    TField tflrTVA;
    boolean ovrset = false;

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        // TODO Auto-generated method stub

        /*
         * // Get TMB Non TVA Target GetCommonParamValue tmbConfig = new
         * GetCommonParamValue(); tmbConfig.AddParam(commonParamId, new String[]
         * { "TVA.TARGET" }); Map<String, Map<String, List<TField>>> TargetList
         * = tmbConfig.GetParamValue(this.DataObj); String[] TargetArray =
         * (String[])
         * TargetList.get(commonParamId).get("SIPEM.PIN").get(0).getValue();
         *
         *
         * 
         * 
         */

        CustomerRecord custRec = new CustomerRecord(currentRecord);

        if (debugg)
            System.out.println("ENtered Default routine ");

        try {
            tflrTVA = custRec.getLocalRefField("L.TVA");
            String tvaOvr = tflrTVA.getOverride().toString();
            if (tvaOvr.length() > 0)
                ovrset = true;

            ltva = custRec.getLocalRefField("L.TVA").getValue();
            lrsc = custRec.getLocalRefField("L.RSC").getValue();
            TargetId = custRec.getTarget().toString();
            TargetId = TargetId.trim();
            int intTarget = Integer.parseInt(TargetId);
            if (debugg)
                System.out.println("Target id " + TargetId);
            if (debugg) {
                String ltva = custRec.getLocalRefField("L.TVA").getValue();
                System.out.println("Value of ltva " + ltva);
            }
            if (debugg)
                System.out.println("Override set " + ovrset);


    
                if ((intTarget == 52) || (intTarget == 53) || (intTarget == 8)) {
                    // Change only if it is blank
                    if (ltva.length() == 0) {
                        if (debugg)
                            System.out.println("Non TVA");
                        // local reference set L.TVA ,L.RSC = No. Else Yes
                        custRec.getLocalRefField("L.TVA").setValue("NON");
                    }
                    if (lrsc.length() == 0) {
                        custRec.getLocalRefField("L.RSC").setValue("NON");
                    }
                } else {
                    if (debugg)
                        System.out.println("Subject to TVA");
                    if (debugg)
                        System.out.println("Length of TVA " + ltva.length());
                    if (ltva.length() == 0) {
                        custRec.getLocalRefField("L.TVA").setValue("OUI");
                    }
                    if (lrsc.length() == 0) {
                        custRec.getLocalRefField("L.RSC").setValue("OUI");
                    }
                }
           

            if (debugg) {
                String ltva = custRec.getLocalRefField("L.TVA").getValue();
                System.out.println("Value of ltva " + ltva);
            }

        } catch (Exception e) {
            System.out.println("Caught exception default rtn---****" + e.getMessage());

        }
        currentRecord.set(custRec.toStructure());
    }

}
