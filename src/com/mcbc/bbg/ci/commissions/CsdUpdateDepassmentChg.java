package com.mcbc.bbg.ci.commissions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.temenos.api.TDate;
import com.temenos.api.TField;
import com.temenos.api.TNumber;
import com.temenos.api.TString;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.Calculation;
import com.temenos.t24.api.party.Account;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.ContextNameClass;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddescharge.CalcTierTypeClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.FromDateClass;
import com.temenos.t24.api.records.ebcontractbalances.EbContractBalancesRecord;
import com.temenos.t24.api.records.limit.LimitRecord;
import com.temenos.t24.api.records.limit.TimeCodeClass;
import com.temenos.t24.api.records.stmtentry.StmtEntryRecord;
import com.temenos.t24.api.records.transaction.TransactionRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.tables.ebcommaccopenbalance.EbCommAccOpenBalanceRecord;
import com.temenos.t24.api.tables.ebcommonparambbgsn.EbCommonParamBbgSnRecord;
import com.temenos.t24.api.tables.ebcommonparambbgsn.ParamNameClass;
import com.temenos.t24.api.tables.ebcommovdtxn.EbCommOvdTxnRecord;
import com.temenos.t24.api.tables.ebcommovdtxn.EbCommOvdTxnTable;
import com.temenos.t24.api.complex.ac.accountapi.*;

/**
 * TODO: Document me!
 *
 * @author suyaga
 *
 */
public class CsdUpdateDepassmentChg extends Calculation {

    DataAccess getDataAccessObject() {
        return new DataAccess(this);
    }

    private String getTodayDate() {
        return new Date(this).getDates().getToday().getValue();
    }

    @Override
    public void calculateAdjustedCharge(String arrangementId, String arrangementCcy, TDate adjustEffectiveDate,
            String adjustChargeProperty, String chargeType, AaPrdDesChargeRecord chargePropertyRecord,
            TNumber adjustBaseAmount, String adjustPeriodStartDate, String adjustPeriodEndDate, String sourceActivity,
            String chargeAmount, String activityId, TNumber adjustChargeAmount, TNumber newChargeAmount,
            TString adjustReason, AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, TStructure productPropertyRecord,
            AaProductCatalogRecord productRecord, TStructure record, AaArrangementActivityRecord masterActivityRecord) {

        String AccNo = null;String ChgProp = null;String Categ = null;String ChRate = "";Double NewChgAmt = 0.0;
        AccountRecord AccRec = new AccountRecord();String LimitId = "";String ChType = "";Double AmtLcy = 0.0;
        StmtEntryRecord StmtEntRec = new StmtEntryRecord();String ArrStDate = "";
        TransactionRecord TxnRec = new TransactionRecord(); Double NewChgAmtComDepass = 0.0;
        EbCommonParamBbgSnRecord ComParRec = new EbCommonParamBbgSnRecord();
        String TodayDate = getTodayDate(); 
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");


        Account Ac = new Account(this);
//        LimitRecord LiRec = new LimitRecord();
        try{     
            AccNo = arrangementRecord.getLinkedAppl(0).getLinkedApplId().getValue().toString();
            ArrStDate  = arrangementRecord.getStartDate().getValue().toString();
            AccRec = new AccountRecord(getDataAccessObject().getRecord("ACCOUNT",AccNo));
            Categ = AccRec.getCategory().getValue().toString();
            System.out.println("Account No = "+AccNo);
            System.out.println("Categ = "+Categ);


        } catch (Exception  e) {}

        try{
            ChgProp = adjustChargeProperty;
            System.out.println("ChgProp = "+ChgProp);
        } catch (Exception  e) {}


        List<ParamNameClass> ParamVal = null;

        try{
            ComParRec = new EbCommonParamBbgSnRecord(getDataAccessObject().getRecord("EB.COMMON.PARAM.BBG.SN","COMMISSION.BBCI"));
            ParamVal = ComParRec.getParamName();
            System.out.println("ParamVal = "+ParamVal);
        } catch (Exception e) {}

        ArrayList<String> CategDepass = new ArrayList<String>();
        ArrayList<String> TxnsExcDepass = new ArrayList<String>();

        try{        
            for (int i=0;i<ParamVal.size();i++)
            {
                String ParamName = ParamVal.get(i).getParamName().getValue().toString();
                if ( ParamName.equalsIgnoreCase("CATEGORY.DEPASSEMENT") ||  ParamName.equalsIgnoreCase("EXCLUDED.TXN.DEPASSE") )
                {

                    if (ParamName.equalsIgnoreCase("CATEGORY.DEPASSEMENT"))
                    {
                        List<TField> ParamValNames = ComParRec.getParamName(i).getParamValue();
                        for (int j=0;j<ParamValNames.size();j++)
                        {
                            CategDepass.add(ComParRec.getParamName(i).getParamValue(j).getValue().toString());
                        }
                    }
                    else if (ParamName.equalsIgnoreCase("EXCLUDED.TXN.DEPASSE"))
                    {
                        List<TField> ParamValNames = ComParRec.getParamName(i).getParamValue();
                        for (int j=0;j<ParamValNames.size();j++)
                        {
                            TxnsExcDepass.add(ComParRec.getParamName(i).getParamValue(j).getValue().toString());
                        }
                    }

                }
            } 

            System.out.println("CategDepass = "+CategDepass);
            System.out.println("TxnsExcDepass = "+TxnsExcDepass);

        }catch (Exception e) {}

        
        
        // GET THE AMTLCY- START
        
        try{
           AmtLcy = Double.parseDouble(arrangementActivityRecord.getOrigTxnAmtLcy().getValue().toString());
        }catch (Exception  e) {}
        
        // GET THE AMTLCY- END
        Double OnlineClearedBal = 0.0;
//        Double limAmt = 0.0;
        Double totLockedAmt = 0.0;
        Double effectiveBalanace = 0.0;
        Double workingbal = 0.0;
        Double netbal = 0.0;
        Double davbal = 0.0;

        try{
            Ac.setAccountId(AccNo);
            System.out.println("Available balance is "+ effectiveBalanace);
            
            OnlineClearedBal = Double.parseDouble(AccRec.getOnlineClearedBal().getValue().toString());
            System.out.println("Online Cleared Balance "+ OnlineClearedBal);
            workingbal = Double.parseDouble(AccRec.getWorkingBalance().toString());
            System.out.println("Working Balance "+ workingbal);
            netbal = OnlineClearedBal - workingbal;
            System.out.println("Net Balance "+ netbal);
            TNumber avbal = Ac.getAvailableBalance().getAmount().getValue();
            davbal = Double.parseDouble(avbal.toString());
            System.out.println("Available Balance "+ davbal);
            Double finbal = davbal + netbal;
            String formattedValue = String.format("%.2f", finbal);
            effectiveBalanace = finbal;
//            OnlineClearedBal = OnlineClearedBal + Math.abs(AmtLcy);

            List<FromDateClass> lockedList = AccRec.getFromDate();

            if (lockedList != null && !lockedList.isEmpty()) { 
                for (FromDateClass lockedDate : lockedList) { 
                    try {
                        String amountStr = lockedDate.getLockedAmount().toString().trim();
                        if (!amountStr.isEmpty()) {
                            double lockedAmt = Double.parseDouble(amountStr);
                            totLockedAmt += lockedAmt;
                        }
                    } catch (NumberFormatException | NullPointerException e) {
                        System.out.println("Could not parse Locked Amount for one record: " + e.getMessage());
                    }
                }
            }
            System.out.println("Final Total Locked Amount is " + totLockedAmt);
            System.out.println("Effective Balance = "+effectiveBalanace );
            System.out.println("Formated Effective Balance = "+formattedValue );
//            limAmt = Double.parseDouble(LiRec.getMaximumTotal().getValue().toString());
//            System.out.println("Limit Amount = "+ limAmt);
        }catch (Exception  e) {System.out.println("fetching working balance exception");}

        //START INITIALIZE VARIABLES

        Double TotTxnAmt = 0.0;List<CalcTierTypeClass> CalcTierType =null;
        Double TotCrAmt = 0.0;String CommMouve = null;Double TotTxnCrAmt = 0.0;Double TotTxnDrAmt = 0.0;
        Double TotDrAmt = 0.0;Double TotChgTxnAmt = 0.0;String SetId = ""; Boolean CollectChg = false;

        //END INITIALIZE VARIABLES
        switch (ChgProp)
        {
        case "BBCICOMDEPASS":

            System.out.println("TodayDate for Comdepass =  "+TodayDate);

            List<ContextNameClass> ContextName = arrangementActivityRecord.getContextName();

            for (int i=0;i<ContextName.size();i++)
            {
                String ContextValue = ContextName.get(i).getContextValue().getValue().toString();
                if (ContextValue.equalsIgnoreCase("CUSTOMER") && !AmtLcy.equals(0.0))
                {
                    CollectChg = true;
                }
            }

            EbCommOvdTxnRecord ComOvdRec = new EbCommOvdTxnRecord();
            EbCommOvdTxnTable ComOvdTab = new EbCommOvdTxnTable(this);
            String SetIdPass = "";
            System.out.println("Collect charges for BBCICOMDEPASS ");
            String FixedChAmt = "";String MinCharge = "";String MinChargeAmt = "";double MinChgAmt = 0.0;
            
            try
            {                
                ChType = chargePropertyRecord.getChargeType().getValue().toString();
                CalcTierType = chargePropertyRecord.getCalcTierType();
                System.out.println("ChType BBCICOMDEPASS= "+ChType);
                if (ChType.equalsIgnoreCase("calculated"))
                {
                    ChRate = CalcTierType.get(0).getChargeRate().getValue().toString();
                    System.out.println("ChRate BBCICOMDEPASS = "+ChRate);
                }
                else if (ChType.equalsIgnoreCase("fixed"))
                {
                    FixedChAmt =  chargePropertyRecord.getFixedAmount().getValue().toString();
                    System.out.println("FixedChAmt = "+FixedChAmt);
                }
                CommMouve = chargePropertyRecord.getLocalRefField("L.COMMOUVE").getValue().toString();
                System.out.println("CommMouve BBCICOMDEPASS= "+CommMouve);

            }catch (Exception  e) {}


            try{
                //START...GET MONTH AND YEAR FROM DATE

                System.out.println("case3 line1") ;   
                LocalDate currentDatePass = LocalDate.parse(adjustPeriodStartDate.toString(),formatter);  
                System.out.println("case3 line2") ; 
                String MonthPass = String.valueOf(currentDatePass.getMonthValue());
                System.out.println("case3 line3") ; 
                String YearPass =  String.valueOf(currentDatePass.getYear());
                System.out.println("case3 line4") ; 


                //END...GET MONTH FROM DATE

                //START...SET ID OF LOCAL TABLE

                SetIdPass = MonthPass+YearPass+"BBCICOMDEPASS"+AccNo;

                //END...SET ID OF LOCAL TABLE

                System.out.println("SetIdPass = "+SetIdPass);

            }catch (Exception  e) {}

            //START...GETTING THE RECORD
            try{
                ComOvdRec = new EbCommOvdTxnRecord(getDataAccessObject().getRecord("EB.COMM.OVD.TXN",SetIdPass));

            }catch (Exception  e) 
            {
                ComOvdRec.setAccountId(AccNo);
            }

            //END...GETTING THE RECORD

            double CurrBal = effectiveBalanace;            
            // Calculate unauth transactions via ECB

            EbContractBalancesRecord ecb = new EbContractBalancesRecord(getDataAccessObject().getRecord("EB.CONTRACT.BALANCES",AccNo));
            double unauthTxn = 0.0;
            String unauthDr = ecb.getTotUnauthDb().toString(); // to get unauthorized debits
            unauthTxn = Double.parseDouble(unauthDr);
            System.out.println("Total Locked Amount = "+totLockedAmt);
            System.out.println("Total Unauth Amount = "+unauthTxn);
            
            if (CollectChg)
            {
                double NewCurrBal = davbal + Math.abs(unauthTxn) + totLockedAmt; // this is to get the new balance after removing the effect of unauth txn and also the efect of locked amount 
                System.out.println("New Current Balance = "+NewCurrBal);
                if (NewCurrBal < 0 )
                {
                    double CurrBalLim = NewCurrBal;
                    System.out.println("CurrBalLim = "+CurrBalLim);
                    if (NewCurrBal < 0)
                    {
                        if (ChType.equalsIgnoreCase("calculated"))
                        {
                            System.out.println("ChType is calulated ");
                            ChRate = CalcTierType.get(0).getChargeRate().getValue().toString();
                            System.out.println("ChRate before calculating charge amount = "+ChRate);
//                            NewChgAmt = (double) Math.round(Math.abs((AmtLcy) * ((Double.parseDouble(ChRate))/100)));
//                          2025-09-09 This change is to calculate the commission based on the new available balance and not the Transaction Amount.  
//                            NewChgAmt = (double) Math.round(Math.abs((CurrBal) * ((Double.parseDouble(ChRate))/100)));
//                            2025-09-18 This change is to calculate based on the new balance after the present transaction
                            NewChgAmt = (double) Math.round(Math.abs((NewCurrBal) * ((Double.parseDouble(ChRate))/100)));
                            System.out.println("NewChgAmt = "+NewChgAmt);
                            try{
                                MinChargeAmt = (CalcTierType.get(0).getTierMinCharge().getValue().toString());
                                MinChgAmt = Double.parseDouble(MinChargeAmt);
                                System.out.println("MinChargeAmt = "+MinChargeAmt);
                            }catch (Exception e){} 
                            
                            try
                            {                                                             
                                if (NewChgAmt > MinChgAmt)
                                {                                   
                                    NewChgAmtComDepass= NewChgAmt;
                                }

                                if (!MinChargeAmt.equals("") && NewChgAmt < MinChgAmt)
                                {                                   
                                    NewChgAmt = MinChgAmt;
                                    NewChgAmtComDepass= NewChgAmt;
                                }
                                

                                System.out.println("NewChgAmtComDepass when ch type is calculated = "+NewChgAmtComDepass);
                            }catch (Exception e){}
                        }
                        
                        else if (ChType.equalsIgnoreCase("fixed"))
                        {
                            FixedChAmt =  chargePropertyRecord.getFixedAmount().getValue().toString();
                            NewChgAmt = Double.parseDouble(FixedChAmt);
                            NewChgAmtComDepass= NewChgAmt;
                            System.out.println("FixedChAmt = "+FixedChAmt);
                            System.out.println("NewChgAmt = "+NewChgAmt);
                        }
                        
                    }    
                    
                }
            }
                       
            break; 
        }

        try{

            if (!NewChgAmt.equals(0.0) )
            {
                
                if ((ChgProp.equals("BBCICOMDEPASS")))
                {
                    System.out.println("Setting the new charge and reason");
                    System.out.println("adjustChargeAmount = "+(NewChgAmt - Double.parseDouble(chargeAmount)));

                    adjustChargeAmount.set(NewChgAmtComDepass - Double.parseDouble(chargeAmount));
                    System.out.println("NewChgAmtComDepass = "+NewChgAmtComDepass);
                    newChargeAmount.set(NewChgAmtComDepass);
                    adjustReason.set("RETAIN.CUSTOMERS.BUSINESS");
                }

            }
        }catch(Exception e ){}


    }
 

}
