package com.mcbc.tmb;

import com.temenos.api.TField;

/**
 * TODO: Document me!
 *
 * @author debdas
 *
 */

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Document me!
 *
 *
 * * /** * @category EB.API - V.VAL.CHECK.LTVA.TMB
 * 
 * @param Application
 *            - CUSTOMER
 * @param VALIDATION.ROUTINE
 *            - Check if the TARGET is 'ambassador'/UN and the value of L.TVA is
 *            OUI then raise Overrides
 * @param Version
 *            - CUSTOMER,INPUT.TMB
 * @return none
 *
 */
public class tmbValidateLtva extends RecordLifecycle {

    public String ltva;
    public String TargetId;
    String OvrValue;
    String CustomerId;
    String tvaFieldName;
    public Boolean debugg = true;
    DataAccess DataObj = new DataAccess(this);
    CustomerRecord custRec;
    TField FieldLTA;
    boolean ovrset = false;
    

    @Override
    public TValidationResponse validateField(String application, String recordId, String fieldData, TStructure record) {
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

        CustomerRecord custRec = new CustomerRecord(record);
        CustomerId = recordId;
        if (debugg)
            System.out.println("Entered Validate Field LTVA routine ");

        try {
            ltva = fieldData;
            TargetId = custRec.getTarget().toString();
            TargetId = TargetId.trim();
            FieldLTA = custRec.getLocalRefField("L.TVA");
            int intTarget = Integer.parseInt(TargetId);
            String tvaOvr = FieldLTA.getOverride().toString();
            if (tvaOvr.length() > 0)
                ovrset = true;
            if (debugg)
                System.out.println("Override set " + ovrset);

            if (debugg)
                System.out.println("Target id " + TargetId);
            if (debugg) {
                String ltva = custRec.getLocalRefField("L.TVA").getValue();
                System.out.println("Value of ltva " + ltva);
            }
/*
            if (!ovrset) {
                if ((intTarget == 52) || (intTarget == 53) || (intTarget == 8)) {
                    if (ltva.equals("OUI")) {
                        tvaFieldName = "L.TVA";
                        RaiseOveridde();
                    }

                } else {
                    if (ltva.equals("NON")) {
                        tvaFieldName = "L.TVA";
                        RaiseOveridde();
                    }

                }
            }

*/           
            if (debugg) {
                String ltva = custRec.getLocalRefField("L.TVA").getValue();
                System.out.println("Value of ltva " + ltva);
            }

        } catch (Exception customerRecordException) {
            System.out.println("Caught exception validation rtn---****" + customerRecordException);

        }

        record.set(custRec.toStructure());
        return custRec.getValidationResponse();
    }

    public void RaiseOveridde() {
        System.out.println("Changes found in  " + tvaFieldName);
        List<String> Ovrlrsc = new ArrayList<>();
        OvrValue = "CG-CUSTOMER.NON.TAX.TMB";
        Ovrlrsc.add(OvrValue);
        Ovrlrsc.add(CustomerId);
        Ovrlrsc.add(tvaFieldName);
        FieldLTA.setOverride(Ovrlrsc.toString());

    }

}
