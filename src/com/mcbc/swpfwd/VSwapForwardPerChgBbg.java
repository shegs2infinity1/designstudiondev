package com.mcbc.swpfwd;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.forex.ChargeCodeClass;
import com.temenos.t24.api.records.forex.ForexRecord;
import com.temenos.t24.api.records.ftcommissiontype.CalcTypeClass;
import com.temenos.t24.api.records.ftcommissiontype.CurrencyClass;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.tafj.api.client.impl.T24Context;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

public class VSwapForwardPerChgBbg extends RecordLifecycle {
  DataAccess da = new DataAccess((T24Context)this);
  
  public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord, TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
    String amtSold = "";
    String fxCcy1 = "";
    String fxCcy2 = "";
    ForexRecord forexRec = new ForexRecord(currentRecord);
    try {
      fxCcy1 = forexRec.getCurrencyBought().getValue();
      fxCcy2 = forexRec.getCurrencySold().getValue();
      if (fxCcy1.equals("XOF")) {
        amtSold = forexRec.getAmountBought().getValue();
      } else if (fxCcy2.equals("XOF")) {
        amtSold = forexRec.getAmountSold().getValue();
      } 
    } catch (Exception var18) {
      System.out.println(var18);
    } 
    if (!amtSold.isEmpty()) {
      String chgCode = "";
      String calcPer = "";
      String procDate = "";
      int chgCodeSize = forexRec.getLocalRefGroups("L.CHG.CODES").size();
      try {
        for (int i = 0; chgCodeSize > i; i++) {
          System.out.println("i:" + i);
          chgCode = "";
          calcPer = "";
          procDate = "";
          LocalRefGroup locRef = forexRec.getLocalRefGroups("L.CHG.CODES").get(i);
          try {
            chgCode = locRef.getLocalRefField("L.CHG.CODES").getValue();
            System.out.println("66:" + chgCode);
          } catch (Exception e) {
            System.out.println(e);
          } 
          try {
            calcPer = locRef.getLocalRefField("L.NEW.COM.TFRS").getValue();
            System.out.println("72:" + calcPer);
          } catch (Exception e) {
            System.out.println(e);
          } 
          try {
            procDate = locRef.getLocalRefField("L.PROC.DATES").getValue();
          } catch (Exception e) {
            System.out.println(e);
          } 
          String commAmt = calcFtCommType(chgCode, calcPer, amtSold);
          System.out.println("78:" + commAmt);
          try {
            LocalRefGroup locRef1 = forexRec.createLocalRefGroup("L.CHG.CODES");
            locRef1.getLocalRefField("L.CHG.CODES").setValue(chgCode);
            try {
              locRef1.getLocalRefField("L.PROC.DATES").setValue(procDate);
            } catch (Exception e) {
              System.out.println(e);
            } 
            try {
              locRef1.getLocalRefField("L.NEW.COM.TFRS").setValue(calcPer);
            } catch (Exception e) {
              System.out.println(e);
            } 
            try {
              locRef1.getLocalRefField("L.CHG.AMOUNTS").setValue(commAmt);
            } catch (Exception e) {
              System.out.println(e);
            } 
            forexRec.getLocalRefGroups("L.CHG.CODES").set(i, locRef1);
          } catch (Exception e) {
            System.out.println("92.1:" + e.getMessage());
          } 
          ChargeCodeClass ccClass = new ChargeCodeClass();
          ccClass.setChargeCode(chgCode);
          ccClass.setChargeAmount(commAmt);
          forexRec.setChargeCode(ccClass, i);
        } 
      } catch (Exception e) {
        System.out.println("92:" + e.getMessage());
      } 
    } 
    currentRecord.set(forexRec.toStructure());
  }
  
  public String calcFtCommType(String chgCode, String calcPercentage, String amtSold) {
    BigDecimal taxtemp = new BigDecimal(0);
    FtCommissionTypeRecord fctRec = null;
    String flatAmt = "";
    BigDecimal fA1 = new BigDecimal(0);
    System.out.println("103:comission started");
    try {
      fctRec = new FtCommissionTypeRecord(this.da.getRecord("FT.COMMISSION.TYPE", chgCode));
    } catch (Exception e) {
      System.out.println("Commission record exception:" + e);
    } 
    try {
      Iterator<CurrencyClass> var3 = fctRec.getCurrency().iterator();
      while (var3.hasNext()) {
        CurrencyClass ccyClass = var3.next();
        String calcType = ((CalcTypeClass)ccyClass.getCalcType().get(0)).toString();
        if (calcType.contains("FLAT")) {
          flatAmt = ccyClass.getFlatAmt().getValue();
          taxtemp = new BigDecimal(flatAmt);
          System.out.println("120:" + taxtemp);
          break;
        } 
        if (calcType.contains("LEVEL")) {
          String flatAmt1 = "";
          try {
            flatAmt1 = ((CalcTypeClass)ccyClass.getCalcType().get(0)).getMinimumAmt().getValue();
            System.out.println("minivalue:" + flatAmt1);
          } catch (Exception exception) {}
          if (!flatAmt1.isEmpty())
            fA1 = new BigDecimal(flatAmt1); 
          String levelPer = "";
          System.out.println("calcPer1:" + calcPercentage);
          if (calcPercentage.isEmpty()) {
            try {
              levelPer = ((CalcTypeClass)ccyClass.getCalcType().get(0)).getPercentage().getValue();
            } catch (Exception exception) {}
          } else {
            levelPer = calcPercentage;
          } 
          System.out.println("levelPer:" + levelPer);
          BigDecimal hunVal = new BigDecimal(100);
          System.out.println("hunVal:" + hunVal);
          BigDecimal levPerDecimal = new BigDecimal(levelPer);
          System.out.println("levPerDecimal:" + levPerDecimal);
          BigDecimal amtSoldDecimal = new BigDecimal(amtSold);
          System.out.println("amtSoldDecimal:" + amtSoldDecimal);
          BigDecimal taxAmnt = amtSoldDecimal.multiply(levPerDecimal);
          System.out.println("taxAmnt:" + taxAmnt);
          taxtemp = taxAmnt.divide(hunVal, 2, RoundingMode.FLOOR);
          System.out.println("taxAmnt:" + taxtemp);
          taxtemp = taxtemp.setScale(0, RoundingMode.FLOOR);
          System.out.println("taxAmnt:" + taxtemp);
          if (fA1.compareTo(taxtemp) > 0)
            taxtemp = fA1; 
        } 
      } 
    } catch (Exception e) {
      System.out.println(" Charge calc exception:" + e);
    } 
    System.out.println("Charge amount:" + taxtemp);
    return taxtemp.toString();
  }
}
