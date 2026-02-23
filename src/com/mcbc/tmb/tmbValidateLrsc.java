
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
 * * /** * @category EB.API - V.VAL.CHECK.LRSC.TMB
 * 
 * @param Application
 *            - CUSTOMER
 * @param VALIDATION.ROUTINE
 *            - Check if the TARGET is 'ambassador'/UN and the value of L.RSC is
 *            OUI then raise Overrides
 * @param Version
 *            - CUSTOMER,INPUT.TMB
 * @return none
 *
 */
public class tmbValidateLrsc extends RecordLifecycle {

    public String lrsc;
    public String TargetId;
    String OvrValue;
    String CustomerId;
    String rscFieldName;
    public Boolean debugg = true;
    DataAccess DataObj = new DataAccess(this);
    CustomerRecord custRec;
    TField FieldRSC;
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
         * TargetList.get(commonParamId).get("TMB.UN").get(0).getValue();
         *
         *
         * 
         * 
         */

        CustomerRecord custRec = new CustomerRecord(record);
        CustomerId = recordId;
        if (debugg)
            System.out.println("Entered Validate Field LRSC routine ");

        try {
            lrsc = fieldData;
            TargetId = custRec.getTarget().toString();
            TargetId = TargetId.trim();
            FieldRSC = custRec.getLocalRefField("L.RSC");

            String tsaOvr = FieldRSC.getOverride().toString();
            if (tsaOvr.length() > 0)
                ovrset = true;
            if (debugg)
                System.out.println("Override set " + ovrset);

            int intTarget = Integer.parseInt(TargetId);
            if (debugg)
                System.out.println("Target id " + TargetId);
            if (debugg) {
                String lrsc = custRec.getLocalRefField("L.RSC").getValue();
                System.out.println("Value of rsc " + lrsc);
            }
/*
            if (!ovrset) {

                if ((intTarget == 52) || (intTarget == 53) || (intTarget == 8)) {
                  if (lrsc.equals("OUI")) {
                        rscFieldName = "L.RSC";
                        RaiseOveriddeSC();
                    }

                } else {
                    if (lrsc.equals("NON")) {
                        rscFieldName = "L.RSC";
                        RaiseOveriddeSC();
                    }

                }

            }

*/            if (debugg) {
                String lrsc = custRec.getLocalRefField("L.RSC").getValue();
                System.out.println("Value of rsc " + lrsc);
            }

        } catch (Exception customerRecordException) {
            System.out.println("Caught exception validation rtn---****" + customerRecordException);

        }

        record.set(custRec.toStructure());
        return custRec.getValidationResponse();
    }

    public void RaiseOveriddeSC() {
        System.out.println("Changes found in  " + rscFieldName);
        List<String> Ovrlrsc = new ArrayList<>();
        OvrValue = "CG-CUSTOMER.NON.TAX.TMB";
        Ovrlrsc.add(OvrValue);
        Ovrlrsc.add(CustomerId);
        Ovrlrsc.add(rscFieldName);
        FieldRSC.setOverride(Ovrlrsc.toString());

    }

}
