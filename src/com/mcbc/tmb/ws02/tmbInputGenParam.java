package com.mcbc.tmb.ws02;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbRecord;
import com.temenos.t24.api.tables.ebwso2paramtmb.EbWso2ParamTmbRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author Debesh /** TODO: Document me!
 *
 *
 *         * /** * @category EB.API -  V.INP.GEN.PARAM.ID.TMB
 * 
 * @param Application
 *            - CUSTOMER
 * @param INPUT.ROUTINE
 *            - form the WSO2.PARAM ID and read the WSO2.PARAM record to get the
 *            version details and also check for duplicate record - update
 *            Version Name , Date Time , Param id
 * @param Version
 *            - VERSION.CONTROL WS02.PAYMENT.DETAILS.TMB
 * @return none
 *
 */

public class tmbInputGenParam extends RecordLifecycle {
    boolean debugg = false;
    EbWs02PaymentDetailsTmbRecord ebwsO2PayRec;
    EbWso2ParamTmbRecord ebwsO2ParamRec;
    DataAccess da = new DataAccess(this);
    String ebwsO2InterfaceType;
    String ebwsO2TransferType;
    String ebwsO2ParamId;
    String ebwsO2MsgId;
    String ebwsO2RMsgId;
    String ReversalType = "INTREVERSE";
    LocalDate currdate = LocalDate.now();
    LocalTime currtime = LocalTime.now();
    Boolean ReversalReq = false;
    Boolean NoData;
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        if (debugg)
            System.out.println("Entering Input General Routine");
        ebwsO2PayRec = new EbWs02PaymentDetailsTmbRecord(currentRecord);
        String VersionName = transactionContext.getCurrentVersionId();
        if (debugg)
            System.out.println("Version Name **** " + VersionName);

        try {
            ebwsO2InterfaceType = ebwsO2PayRec.getInterfaceType().toString();
            ebwsO2TransferType = ebwsO2PayRec.getTransferType().toString();
            ebwsO2ParamId = ebwsO2InterfaceType.trim() + "-" + ebwsO2TransferType.trim();
            ebwsO2MsgId = ebwsO2PayRec.getMsgId().toString();
            if (ebwsO2TransferType.equals(ReversalType)) {
                if (debugg) System.out.println("Reversal type");
                ReversalReq = true;
                ebwsO2RMsgId = ebwsO2MsgId.substring(1);
                if (debugg) System.out.println("Reversal id " + ebwsO2RMsgId);
                getEBWS02RecDetails();

            }

           
            if (debugg)
                System.out.println("ebwsO2PayRec" + ebwsO2PayRec);

            Boolean dupStat = checkDuplicateMsg();
//            System.out.println("Duplicate Status " + dupStat);
            if (dupStat) {
                if (ReversalReq) {
//                    System.out.println("No  Msg.id to reverse");
                    ebwsO2PayRec.getMsgId().setError("MsgID Does not exists ");
                } else {
//                    System.out.println("Duplicate Msg.id");
                    ebwsO2PayRec.getMsgId().setError("EB-DUP.MSG.ID");
                }
            }

        } catch (Exception e) {
            System.out.println("Exception error EB WSO2 Payment Detail " + e);
        }

        try {
            String DtTime = currdate.toString() + " " + currtime.toString().substring(0, 7);
//            System.out.println("DateTime  " + DtTime);
            EbWso2ParamTmbRecord ebwsO2ParamRec = new EbWso2ParamTmbRecord(
                    da.getRecord("EB.WSO2.PARAM.TMB", ebwsO2ParamId));
            String versionId = ebwsO2ParamRec.getVersionName().toString();
            String creditAmt = ebwsO2PayRec.getDebitAmt().toString();
            ebwsO2PayRec.setParamId(ebwsO2ParamId);
            ebwsO2PayRec.setDtCreated(DtTime);
            //ebwsO2PayRec.setCreditAmt(creditAmt);
            ebwsO2PayRec.setVersionName(versionId);
        } catch (Exception eparam) {
//            System.out.println("Exception error EB WSO2 Payment Detail " + eparam);
            ebwsO2PayRec.getTransferType().setError("Invalid Transfer Type / Interface Type");
        }

        currentRecord.set(ebwsO2PayRec.toStructure());
        return ebwsO2PayRec.getValidationResponse();

    }

    /**
     * 
     */
    private void getEBWS02RecDetails() {
        // TODO Auto-generated method stub
        try {
            if (debugg)
                System.out.println("Checking Reversal Record");
            EbWs02PaymentDetailsTmbRecord ebwsO2PayRecRev = new EbWs02PaymentDetailsTmbRecord(
                    da.getRecord("EB.WS02.PAYMENT.DETAILS.TMB", ebwsO2RMsgId));
            String RevTxnId = ebwsO2PayRecRev.getTranId().toString();
            ebwsO2PayRec.setTranId(RevTxnId);
        } catch (Exception e) {
//            System.out.println("Reversal Record error " + e);
        }
    }

    /**
     * 
     */
    private boolean checkDuplicateMsg() {
        // TODO Auto-generated method stub
        boolean ErrStatus = true;
      
        String ReqId = ebwsO2MsgId;
        try {
            if (ReversalReq) {
                if (debugg)
                    System.out.println("Reversal type");
                ReqId = ebwsO2RMsgId;
            }

            List<String> recidsPaydets = da.selectRecords("", "EB.WS02.PAYMENT.DETAILS.TMB", "",
                    "WITH @ID EQ " + ReqId + " BY-DSND @ID");
            if (debugg)
                System.out.println("recidsPaydets" + recidsPaydets);
            String cmdSel = "EB.WS02.PAYMENT.DETAILS.TMB" + "" + "WITH @ID EQ " + ReqId + " BY-DSND @ID";
//            System.out.println(cmdSel);
            NoData=recidsPaydets.isEmpty();
            int norecidsPaydets = recidsPaydets.size();
//            System.out.println("No of records available " + norecidsPaydets);
//            System.out.println("Reversal Status :" + ReversalReq);
           
//                System.out.println("No Record exists " + NoData);
                
            int caseStatus = 9 ;
            
            if (!NoData && ReversalReq)    caseStatus = 1  ;
            
            if (NoData && !ReversalReq)  caseStatus = 2;
            
//            System.out.println("Final caseStatus " + caseStatus);
            switch (caseStatus) {
            case 1: ErrStatus = false;
            break;
            case 2: ErrStatus = false;
            break;
            default : ErrStatus = true;
            break;
            }
//            System.out.println("Final Error Status " + ErrStatus);

        } catch (Exception erSelect) {
//            System.out.println("Error Selecting " + erSelect);
        }
        
        return ErrStatus;

    }

}
