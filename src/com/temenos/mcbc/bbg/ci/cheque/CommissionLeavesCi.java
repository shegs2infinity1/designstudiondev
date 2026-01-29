package com.temenos.mcbc.bbg.ci.cheque;

import com.mcbc.bbg.sn.common.utils.GetParamValueBbgSn;
import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.InputValue;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.chequeissue.ChequeIssueRecord;
import com.temenos.t24.api.records.chequeissue.ChgCodeClass;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebcommonparambbgsn.EbCommonParamBbgSnRecord;
import com.temenos.t24.api.tables.ebcommonparambbgsn.ParamNameClass;
import com.temenos.tafj.api.client.impl.T24Context;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommissionLeavesCi extends RecordLifecycle {
  public String paramCurrency;
  
  public String paramCompany;
  
  public String paramCommission;
  
  public void defaultFieldValuesOnHotField(String application, String currentRecordId, TStructure currentRecord, InputValue currentInputValue, TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
    
//      System.out.println("Getting the Charge Conditions");
//      
//    ChequeIssueRecord chequeIssueRecord = new ChequeIssueRecord(currentRecord);
//    String chargeAmount = "";
//    DataAccess DataOjb = new DataAccess((T24Context)this);
//    String commonParamId = "CHEQUE.TYPE.EXCLUDE";
//   
//    try {
//      Session SessionContext = new Session((T24Context)this);
//      String[] chequetype = currentRecordId.split(".");
//      String chqtype = chequetype[0];
//      System.out.println("ChequeType is "+chqtype);
//      EbCommonParamBbgSnRecord bbgComParamRec = new EbCommonParamBbgSnRecord();
//      bbgComParamRec = new EbCommonParamBbgSnRecord(DataOjb.getRecord("EB.COMMON.PARAM.BBG.SN", commonParamId));
//      List<ParamNameClass> pnclassList = bbgComParamRec.getParamName();
//      List<TField> chqtypeParamList = new ArrayList<>();
//      List<String> ParamList = new ArrayList<>();
//      for (ParamNameClass pnclass : pnclassList) {
//          String ParamName = pnclass.getParamName().toString();
//            System.out.println("ParamName got " + ParamName); 
//          List<TField> PvalueList = pnclass.getParamValue();
//          if (ParamName.equals("CHEQUE.TYPE.EXCLUDE"))
//            chqtypeParamList = PvalueList; 
//        }
//      for (TField ParmTxnid : chqtypeParamList) { 
//          ParamList.add(ParmTxnid.toString());
//        } 
//      if (ParamList.contains(chqtype)){
//          chequeIssueRecord.setWaiveCharges("NO");
//          currentRecord.set(chequeIssueRecord.toStructure());
//          return;
//      };
//      
//      
//      String numOfLeaves = chequeIssueRecord.getLocalRefField("L.NUM.ISSUED").getValue();
//      if (!numOfLeaves.isEmpty()) {
//        T24Context EcpBbg = new T24Context("EB.COMMON.PARAM.BBG.SN");
//        
//        GetParamValueBbgSn Config = new GetParamValueBbgSn();
//        Config.AddParam("CHEQUE", new String[] { "CHQ.CHARGES" });
//        Map<String, Map<String, List<TField>>> ParamConfig = Config.GetParamValue(DataOjb);
//        for (int i = 0; ((List)((Map)ParamConfig.get("CHEQUE")).get("CHQ.CHARGES")).size() > i; i++) {
//          String[] tempValues = ((TField)((List<TField>)((Map)ParamConfig.get("CHEQUE")).get("CHQ.CHARGES")).get(i)).getValue().split("-");
//          String leaves = Array.get(tempValues, 0).toString();
//          String chequeType = Array.get(tempValues, 1).toString();
//          if (leaves.equals(numOfLeaves))
//            this.paramCommission = chequeType; 
//        } 
//        System.out.println("-----");
//        System.out.println("------------");
//        FtCommissionTypeRecord ftCommissionTypeRecord = new FtCommissionTypeRecord(DataOjb.getRecord("FT.COMMISSION.TYPE", this.paramCommission));
//        String ChqIssueCcy = chequeIssueRecord.getCurrency().getValue();
//        for (int j = 0; ftCommissionTypeRecord.getCurrency().size() > j; j++) {
//          String ftComissionTypeRec = ftCommissionTypeRecord.getCurrency(j).getCurrency().getValue();
//          if (ftComissionTypeRec.equals(ChqIssueCcy))
//            chargeAmount = ftCommissionTypeRecord.getCurrency(j).getFlatAmt().getValue(); 
//        } 
//        ChgCodeClass chargeCodeClass = new ChgCodeClass();
//        chargeCodeClass.setChgCode(this.paramCommission);
//        chargeCodeClass.setChgAmount(chargeAmount);
//        chequeIssueRecord.setChgCode(chargeCodeClass, 0);
//        System.out.println("----commmiss--------");
//        System.out.println(this.paramCommission);
//      } 
//    } catch (Exception exception) {}
//    currentRecord.set(chequeIssueRecord.toStructure());
  }

@Override
public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
        TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
    System.out.println("Getting the Charge Record From Parameter");
    
    ChequeIssueRecord chequeIssueRecord = new ChequeIssueRecord(currentRecord);
    String chargeAmount = "";
    DataAccess DataOjb = new DataAccess(this);
    String commonParamId = "CHEQUE.TYPE.EXCLUDE";
    String[] chequetype = currentRecordId.split("\\.");
    System.out.println("Current Record ID "+currentRecordId);
    String chqtype = chequetype[0];
    System.out.println("ChequeType is "+chqtype);
   
    try {
      EbCommonParamBbgSnRecord bbgComParamRec = new EbCommonParamBbgSnRecord();
      bbgComParamRec = new EbCommonParamBbgSnRecord(DataOjb.getRecord("EB.COMMON.PARAM.BBG.SN", commonParamId));
      List<ParamNameClass> pnclassList = bbgComParamRec.getParamName();
      List<TField> chqtypeParamList = new ArrayList<>();
      List<String> ParamList = new ArrayList<>();
      for (ParamNameClass pnclass : pnclassList) {
          String ParamName = pnclass.getParamName().toString();
            System.out.println("ParamName got " + ParamName); 
          List<TField> PvalueList = pnclass.getParamValue();
          if (ParamName.equals("CHEQUE.TYPE.EXCLUDE"))
            chqtypeParamList = PvalueList; 
        }
      for (TField ParmTxnid : chqtypeParamList) { 
          ParamList.add(ParmTxnid.toString());
        } 
      if (ParamList.contains(chqtype)){
          chequeIssueRecord.setWaiveCharges("YES");
          currentRecord.set(chequeIssueRecord.toStructure());
          return;
      };
      
      
      String numOfLeaves = chequeIssueRecord.getLocalRefField("L.NUM.ISSUED").getValue();
      
      System.out.println("Number of Leaves "+numOfLeaves);
      
      if (!numOfLeaves.isEmpty()) {
        T24Context EcpBbg = new T24Context("EB.COMMON.PARAM.BBG.SN");
        
        GetParamValueBbgSn Config = new GetParamValueBbgSn();
        Config.AddParam("CHEQUE", new String[] { "CHQ.CHARGES" });
        Map<String, Map<String, List<TField>>> ParamConfig = Config.GetParamValue(DataOjb);
        for (int i = 0; ((List)((Map)ParamConfig.get("CHEQUE")).get("CHQ.CHARGES")).size() > i; i++) {
          String[] tempValues = ((TField)((List<TField>)((Map)ParamConfig.get("CHEQUE")).get("CHQ.CHARGES")).get(i)).getValue().split("-");
          String leaves = Array.get(tempValues, 0).toString();
          String chequeType = Array.get(tempValues, 1).toString();
          if (leaves.equals(numOfLeaves))
            this.paramCommission = chequeType; 
        } 
        System.out.println("-----");
        System.out.println("------------");
        FtCommissionTypeRecord ftCommissionTypeRecord = new FtCommissionTypeRecord(DataOjb.getRecord("FT.COMMISSION.TYPE", this.paramCommission));
        String ChqIssueCcy = chequeIssueRecord.getCurrency().getValue();
        for (int j = 0; ftCommissionTypeRecord.getCurrency().size() > j; j++) {
          String ftComissionTypeRec = ftCommissionTypeRecord.getCurrency(j).getCurrency().getValue();
          if (ftComissionTypeRec.equals(ChqIssueCcy))
            chargeAmount = ftCommissionTypeRecord.getCurrency(j).getFlatAmt().getValue(); 
        } 
        ChgCodeClass chargeCodeClass = new ChgCodeClass();
        chargeCodeClass.setChgCode(this.paramCommission);
        chargeCodeClass.setChgAmount(chargeAmount);
        chequeIssueRecord.setChgCode(chargeCodeClass, 0);
        System.out.println("----commmiss--------");
        System.out.println(this.paramCommission);
      } 
    } catch (Exception exception) {}
    currentRecord.set(chequeIssueRecord.toStructure());
}
}
