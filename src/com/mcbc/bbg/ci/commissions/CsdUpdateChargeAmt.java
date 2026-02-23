package com.mcbc.bbg.ci.commissions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.temenos.t24.api.complex.ac.accountapi.Amount;
import com.temenos.t24.api.hook.arrangement.Calculation;
import com.temenos.t24.api.party.Account;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aainterestaccruals.AaInterestAccrualsRecord;
import com.temenos.t24.api.records.aainterestaccruals.FromDateClass;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddescharge.CalcTierTypeClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.transaction.TransactionRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.limit.LimitRecord;
import com.temenos.t24.api.records.limit.TimeCodeClass;
import com.temenos.t24.api.records.stmtentry.StmtEntryRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.tables.ebcommaccopenbalance.EbCommAccOpenBalanceRecord;
import com.temenos.t24.api.tables.ebcommaccopenbalance.EbCommAccOpenBalanceTable;
import com.temenos.t24.api.tables.ebcommcrdrtxn.EbCommCrDrTxnRecord;
import com.temenos.t24.api.tables.ebcommcrdrtxn.EbCommCrDrTxnTable;
import com.temenos.t24.api.tables.ebcommcrdrtxn.TransactionRefClass;
import com.temenos.t24.api.tables.ebcommhighestovdtxn.EbCommHighestOvdTxnRecord;
import com.temenos.t24.api.tables.ebcommhighestovdtxn.EbCommHighestOvdTxnTable;
import com.temenos.t24.api.tables.ebcommonparambbgsn.EbCommonParamBbgSnRecord;
import com.temenos.t24.api.tables.ebcommonparambbgsn.ParamNameClass;
import com.temenos.t24.api.tables.ebcommovdtxn.EbCommOvdTxnRecord;
import com.temenos.t24.api.tables.ebcommovdtxn.EbCommOvdTxnTable;
import com.temenos.t24.api.tables.eblegacycommissionamt.*;

/**
 * TODO: Document me!
 *
 * @author suyaga
 *
 */

public class CsdUpdateChargeAmt extends Calculation {

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

        try {
            System.out.println("arrangementId = " + arrangementId);
//            System.out.println("arrangementCcy = " + arrangementCcy);
//            System.out.println("adjustEffectiveDate = " + adjustEffectiveDate);
//            System.out.println("adjustChargeProperty = " + adjustChargeProperty);
//            System.out.println("chargeType = " + chargeType);
//            System.out.println("chargePropertyRecord = " + chargePropertyRecord);
//            System.out.println("adjustBaseAmount = " + adjustBaseAmount);
//            System.out.println("adjustPeriodStartDate = " + adjustPeriodStartDate);
//            System.out.println("adjustPeriodEndDate = " + adjustPeriodEndDate);
//            System.out.println("chargeAmount = " + chargeAmount);
//            System.out.println("activityId = " + activityId);
//            System.out.println("adjustChargeAmount = " + adjustChargeAmount);
//            System.out.println("newChargeAmount = " + newChargeAmount);
//            System.out.println("arrangementActivityRecord = " + arrangementActivityRecord);
        } catch (Exception e) {
        }

        String AccNo = null;
        String ChgProp = null;
        String Categ = null;
        String ChRate = "";
        Double NewChgAmt = 0.0;
        AccountRecord AccRec = new AccountRecord();
        String LimitId = "";
        String ChType = "";
        StmtEntryRecord StmtEntRec = new StmtEntryRecord();
        String ArrStDate = "";
        TransactionRecord TxnRec = new TransactionRecord();
        Double NewChgAmtComDepass = 0.0;
        EbCommonParamBbgSnRecord ComParRec = new EbCommonParamBbgSnRecord();
        String TodayDate = getTodayDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        boolean eomCob = false;
        int Iyear = 0;
        int Imonth = 0;
        String CoBStatus = null;
        try {

            // String CompanyCode =
            // arrangementActivityRecord.getArrCompanyCode().toString();
            String CompanyCode = arrangementRecord.getCoCodeRec().getValue();
            // System.out.println("Company Code "+ CompanyCode);
            String orgSystemDate = arrangementActivityRecord.getOrgSystemDate().toString();
            DatesRecord CobDatesRec = new DatesRecord(getDataAccessObject().getRecord("DATES", CompanyCode + "-COB"));
            DatesRecord DatesRec = new DatesRecord(getDataAccessObject().getRecord("DATES", CompanyCode));
            // System.out.println("Date Rec "+ DatesRec);
            CoBStatus = DatesRec.getCoBatchStatus().toString();
            System.out.println("COB Status  " + CoBStatus);

            String Cobday = CobDatesRec.getToday().toString();
            String Curday = DatesRec.getToday().toString(); // 20250602
            String curMonth = Curday.substring(4, 6); // 06
            String PeriodEnd = DatesRec.getLastPeriodEnd().toString(); // 20250531
            String PeriodEndMonth = PeriodEnd.substring(4,6); // 05
            

            System.out.println("Today  " + Curday);
            System.out.println("Last Period  " + PeriodEnd);
            System.out.println("Last Working Day  " + Cobday);

            String Syear = DatesRec.getLastPeriodEnd().toString().substring(0, 4);
            String Smonth = DatesRec.getLastPeriodEnd().toString().substring(4, 6);
            
            Iyear = Integer.parseInt(Syear);
            Imonth = Integer.parseInt(Smonth);
            // 06 != 05
            if (!curMonth.equals(PeriodEndMonth)){
                eomCob = true;
                System.out.println("++++++++++++++++++++++++++++EOM COB++++++++++++++-----------------------" + eomCob);
            }
            

        } catch (Exception e) {

        }

        Map<String, LocalDate> Caldates = getFirstAndLastDateOfMonth(Iyear, Imonth);
        LocalDate calfirstDay = Caldates.get("firstDate");
        String CalendarFirstDay = calfirstDay.format(formatter);
        TDate CalTdateFirst = new TDate(CalendarFirstDay);
        LocalDate calLastDay = Caldates.get("lastDate");
        String CalendarLastDay = calLastDay.format(formatter);
        TDate CalTdateLast = new TDate(CalendarLastDay);
        System.out.println("First and last " + CalendarFirstDay + " and " + CalendarLastDay);
        adjustPeriodStartDate = CalendarFirstDay;
        adjustPeriodEndDate = CalendarLastDay;
        // setting cob status for testing
        // CoBStatus = "B";
        // eomCob = true;
        // adjustPeriodStartDate = "20250301";
        // adjustPeriodEndDate = "20250331";
        if ("B".equals(CoBStatus) && eomCob) {
            System.out.println("EOM COB Charge Processing  =====================================================================");
            Account Ac = new Account(this);
            LimitRecord LiRec = new LimitRecord();
            try {
                AccNo = arrangementRecord.getLinkedAppl(0).getLinkedApplId().getValue().toString();
                ArrStDate = arrangementRecord.getStartDate().getValue().toString();
                AccRec = new AccountRecord(getDataAccessObject().getRecord("ACCOUNT", AccNo));
                Categ = AccRec.getCategory().getValue().toString();
                try {
                    LimitId = AccRec.getLimitKey().getValue().toString();
                    LiRec = new LimitRecord(getDataAccessObject().getRecord("LIMIT", LimitId));
                    System.out.println("LimitId = " + LimitId);
                } catch (Exception e) {
                }
                System.out.println("Account No = " + AccNo);
                System.out.println("Categ = " + Categ);

            } catch (Exception e) {
            }

            try {
                ChgProp = adjustChargeProperty;
//                System.out.println("ChgProp = " + ChgProp);
            } catch (Exception e) {
            }

            // START*GET THE VALID CATEGORY OF ACC FROM PARAM TABLE//

            List<ParamNameClass> ParamVal = null;

            try {
                ComParRec = new EbCommonParamBbgSnRecord(
                        getDataAccessObject().getRecord("EB.COMMON.PARAM.BBG.SN", "COMMISSION.BBCI"));
                ParamVal = ComParRec.getParamName();
//                System.out.println("ParamVal = " + ParamVal);
            } catch (Exception e) {
            }

            ArrayList<String> CategMouv = new ArrayList<String>();
            ArrayList<String> CategDepass = new ArrayList<String>();
            ArrayList<String> CategDecouv = new ArrayList<String>();
            ArrayList<String> TxnsExcMouve = new ArrayList<String>();
            ArrayList<String> TxnsExcDepass = new ArrayList<String>();

            try {
                for (int i = 0; i < ParamVal.size(); i++) {
                    String ParamName = ParamVal.get(i).getParamName().getValue().toString();
                    if (ParamName.equalsIgnoreCase("CATEGORY.MOUVEMENT")
                            || ParamName.equalsIgnoreCase("TXNS.EXCLUDE.MOUVE")
                            || ParamName.equalsIgnoreCase("CATEGORY.DEPASSEMENT")
                            || ParamName.equalsIgnoreCase("EXCLUDED.TXN.DEPASSE")
                            || ParamName.equalsIgnoreCase("CATEGORY.DECOUVERT")
                            || ParamName.equalsIgnoreCase("CATEGORY.DECOUVERT")) {
                        if (ParamName.equalsIgnoreCase("CATEGORY.MOUVEMENT")) {
                            List<TField> ParamValNames = ComParRec.getParamName(i).getParamValue();
                            for (int j = 0; j < ParamValNames.size(); j++) {
                                CategMouv.add(ComParRec.getParamName(i).getParamValue(j).getValue().toString());
                            }
                        } else if (ParamName.equalsIgnoreCase("TXNS.EXCLUDE.MOUVE")) {
                            List<TField> ParamValNames = ComParRec.getParamName(i).getParamValue();
                            for (int j = 0; j < ParamValNames.size(); j++) {
                                TxnsExcMouve.add(ComParRec.getParamName(i).getParamValue(j).getValue().toString());
                            }
                        } else if (ParamName.equalsIgnoreCase("CATEGORY.DEPASSEMENT")) {
                            List<TField> ParamValNames = ComParRec.getParamName(i).getParamValue();
                            for (int j = 0; j < ParamValNames.size(); j++) {
                                CategDepass.add(ComParRec.getParamName(i).getParamValue(j).getValue().toString());
                            }
                        } else if (ParamName.equalsIgnoreCase("EXCLUDED.TXN.DEPASSE")) {
                            List<TField> ParamValNames = ComParRec.getParamName(i).getParamValue();
                            for (int j = 0; j < ParamValNames.size(); j++) {
                                TxnsExcDepass.add(ComParRec.getParamName(i).getParamValue(j).getValue().toString());
                            }
                        } else if (ParamName.equalsIgnoreCase("CATEGORY.DECOUVERT")) {
                            List<TField> ParamValNames = ComParRec.getParamName(i).getParamValue();
                            for (int j = 0; j < ParamValNames.size(); j++) {
                                CategDecouv.add(ComParRec.getParamName(i).getParamValue(j).getValue().toString());
                            }
                        }
                    }
                }

//                System.out.println("CategMouv = " + CategMouv);
//                System.out.println("CategDepass = " + CategDepass);
//                System.out.println("CategDecouv = " + CategDecouv);
//                System.out.println("TxnsExcMouve = " + TxnsExcMouve);
//                System.out.println("TxnsExcDepass = " + TxnsExcDepass);

            } catch (Exception e) {
            }

            // END*GET THE VALID CATEGORY OF ACC FROM PARAM TABLE//

            Double OpenBal = 0.0;
            EbCommAccOpenBalanceRecord OpBalRec = new EbCommAccOpenBalanceRecord();
            EbCommAccOpenBalanceTable OpBalTab = new EbCommAccOpenBalanceTable(this);

            try {

                Ac.setAccountId(AccNo);
                try {
                    OpBalRec = new EbCommAccOpenBalanceRecord(
                            getDataAccessObject().getRecord("EB.COMM.ACC.OPEN.BALANCE", AccNo));
                    OpenBal = Double.parseDouble(OpBalRec.getOpenBalance().getValue().toString());
                } catch (Exception e) {
                }

                System.out.println("OpenBal = " + OpenBal);
            } catch (Exception e) {
            }

            List<String> StmtEntList = new ArrayList<>();
            TDate StDate = null;
            TDate EndDate = null;
            try {

                StDate = new TDate(adjustPeriodStartDate);
                EndDate = new TDate(adjustPeriodEndDate);
                System.out.println("StDate = " + StDate);
                System.out.println("EndDate = " + EndDate);

            } catch (Exception e) {
            }
            TDate EDay = null;
            String EeDay = null;
            String StDay = null;
            TDate SDay = null;

            if (ChgProp.equalsIgnoreCase("BBCICOMMOUVE")) {
                try {

//                    System.out.println("L1");
//                    System.out.println("L2");
                    LocalDate StartDay = LocalDate.parse(StDate.toString(), formatter);
//                    System.out.println("L2");
//                    System.out.println("StartDay = " + StartDay);
                    StDay = StartDay.plusDays(1).format(formatter);
//                    System.out.println("L3");
//                    System.out.println("StDay after addition = " + StDay);
                    SDay = new TDate(StDay);
//                    System.out.println("L4");
                    if (ArrStDate.equals(StDate.toString())) {
                        StmtEntList = Ac.getEntries("BOOK", "", "", "", StDate, EndDate);
                    } else {
                        StmtEntList = Ac.getEntries("BOOK", "", "", "", SDay, EndDate);
                    }
//                    System.out.println("L6");
//                    System.out.println("StmtEntList = " + StmtEntList);

                } catch (Exception e) {
                }
            }

            if (ChgProp.equalsIgnoreCase("BBCICOMDECOU")) {
                try {
//                    System.out.println("L1");
                    LocalDate EndDay = LocalDate.parse(EndDate.toString(), formatter);
//                    System.out.println("L2");
//                    System.out.println("EndDay = " + EndDay);
                    EeDay = EndDay.minusDays(1).format(formatter);
//                    System.out.println("L3");
//                    System.out.println("EeDay = " + EeDay);
                    EDay = new TDate(EeDay);
//                    System.out.println("L4");
//                    System.out.println("End Date after subtraction = " + EDay);
//                    System.out.println("L5");
                    StmtEntList = Ac.getEntries("VALUE", "", "", "", StDate, EDay);
//                    System.out.println("L6");
//                    System.out.println("StmtEntList = " + StmtEntList);

                } catch (Exception e) {
                }
            }

            else if (ChgProp.equalsIgnoreCase("BBCICOMDEPASS")) {
                try {
                    
                } catch (Exception e) {
                }
            }
            // START INITIALIZE VARIABLES

            Double TotTxnAmt = 0.0;
            List<CalcTierTypeClass> CalcTierType = null;
            Double TotCrAmt = 0.0;
            String CommMouve = null;
            Double TotTxnCrAmt = 0.0;
            Double TotTxnDrAmt = 0.0;
            Double TotDrAmt = 0.0;
            Double TotChgTxnAmt = 0.0;
            String SetId = "";

            // END INITIALIZE VARIABLES

            switch (ChgProp) {
            case "BBCICOMMOUVE":

                EbCommCrDrTxnRecord ComCrDrRec = new EbCommCrDrTxnRecord();
                EbCommCrDrTxnTable ComCrDrTab = new EbCommCrDrTxnTable(this);

                System.out.println("Collect charges for BBCICOMMOUVE ");
                try {

                    CalcTierType = chargePropertyRecord.getCalcTierType();
                    ChRate = CalcTierType.get(0).getChargeRate().getValue().toString();
                    CommMouve = chargePropertyRecord.getLocalRefField("L.COMMOUVE").getValue().toString();
                    System.out.println("CalcTierType BBCICOMMOUVE= " + CalcTierType);
                    System.out.println("ChRate BBCICOMMOUVE = " + ChRate);
                    System.out.println("CommMouve BBCICOMMOUVE= " + CommMouve);

                } catch (Exception e) {
                }

                // START...GET MONTH AND YEAR FROM DATE

                try {
                    System.out.println("case1 line1 change1");
                    LocalDate currentDateComm = LocalDate.parse(adjustPeriodStartDate.toString(), formatter);
                    System.out.println("case1 line2");
                    String MonthComm = String.valueOf(currentDateComm.getMonthValue());
                    System.out.println("case1 line3");
                    String YearComm = String.valueOf(currentDateComm.getYear());
                    System.out.println("case1 line4");
                    System.out.println("currentDateComm = " + currentDateComm);
                    System.out.println("case1 line5");
                    System.out.println("MonthComm = " + MonthComm);
                    System.out.println("case1 line6");
                    System.out.println("YearComm = " + YearComm);
                    System.out.println("case1 line7");
                    // START...SET ID OF LOCAL TABLE
                    SetId = MonthComm + YearComm + "COMMOUVE" + AccNo;
                    System.out.println("SetId = " + SetId);
                    // END...SET ID OF LOCAL TABLE

                } catch (Exception e) {
                    System.out.println("Exception getting month, date or year");
                }
                // END...GET MONTH AND YEAR FROM DATE

                // START...GET THE RECORD
                try {
                    // ComCrDrRec = new
                    // EbCommCrDrTxnRecord(getDataAccessObject().getRecord("EB.COMM.CR.DR.TXN",SetId));
                    ComCrDrRec = new EbCommCrDrTxnRecord();

                } catch (Exception e) {
                    ComCrDrRec.setAccountId(AccNo);
                }
                // END...GET THE RECORD

                try {
                    if (!ChRate.equals("0") && !ChRate.equals("0.00") && !ChRate.equals(""))
//                    if (!ChRate.equals("0") && !ChRate.equals("")) // check if
                                                                   // charge
                                                                   // rate is
                                                                   // not equal
                                                                   // to 0 and
                                                                   // not null;
                    {
                        for (int i = 0; i < StmtEntList.size(); i++) {

                            String StmtEntId = StmtEntList.get(i);
                            System.out.println("StmtEntId = " + StmtEntId);
                            StmtEntRec = new StmtEntryRecord(getDataAccessObject().getRecord("STMT.ENTRY", StmtEntId));
                            String TxnCode = StmtEntRec.getTransactionCode().getValue().toString();
//                            System.out.println("TxnCode = " + TxnCode);
                            String TxnValDate = StmtEntRec.getValueDate().getValue().toString();
//                            System.out.println("TxnValDate = " + TxnValDate);
                            Double AmtLcy = Double.parseDouble(StmtEntRec.getAmountLcy().getValue().toString());
//                            System.out.println("AmtLcy = " + AmtLcy);
                            TransactionRecord TranRec = new TransactionRecord();
                            TranRec = new TransactionRecord(getDataAccessObject().getRecord("TRANSACTION", TxnCode));
                            String Init = TranRec.getInitiation().getValue().toString();
//                            System.out.println("Init = " + Init);

                            if (Init.equalsIgnoreCase("CUSTOMER") && CategMouv.contains(Categ)
                                    && !TxnsExcMouve.contains(TxnCode)) {
                                if (CommMouve.equalsIgnoreCase("default")) {
                                    TotTxnAmt = TotTxnAmt + Math.abs((AmtLcy));
//                                    System.out.println("TotTxnAmt = " + TotTxnAmt);
//                                    System.out.println("CommMouve = " + CommMouve);

                                    // START***SETTING THE VALUES IN LOCAL TABLE

                                    ComCrDrRec.setAccountId(AccNo);
                                    TransactionRefClass param = new TransactionRefClass();
                                    param.setTransactionRef(StmtEntId);
                                    param.setAmount(AmtLcy.toString());
                                    param.setDate(TxnValDate);

                                    if (AmtLcy > 0) {
                                        TotTxnCrAmt += AmtLcy;
                                        param.setDebCre("CR");
                                    } else if (AmtLcy < 0) {
                                        TotTxnDrAmt += AmtLcy;
                                        param.setDebCre("DR");
                                    }
                                    ComCrDrRec.addTransactionRef(param);
//                                    System.out.println("ComCrDrRec = " + ComCrDrRec);

                                    // END***SETTING THE VALUES IN LOCAL TABLE

                                } else if (CommMouve.equalsIgnoreCase("credit") && (AmtLcy > 0)) {
                                    TotCrAmt += AmtLcy;
//                                    System.out.println("TotTxnAmt = " + TotTxnAmt);
//                                    System.out.println("CommMouve = " + CommMouve);

                                    // START***SETTING THE VALUES IN LOCAL TABLE

                                    ComCrDrRec.setAccountId(AccNo);
                                    TransactionRefClass param = new TransactionRefClass();
                                    param.setTransactionRef(StmtEntId);
                                    param.setAmount(AmtLcy.toString());
                                    param.setDate(TxnValDate);
                                    param.setDebCre("CR");
                                    ComCrDrRec.addTransactionRef(param);
//                                    System.out.println("ComCrDrRec = " + ComCrDrRec);

                                    // END***SETTING THE VALUES IN LOCAL TABLE
                                } else if (CommMouve.equalsIgnoreCase("DR ONLY") && (AmtLcy < 0)) {
                                    TotDrAmt += AmtLcy;
//                                    System.out.println("TotTxnAmt = " + TotTxnAmt);
//                                    System.out.println("CommMouve = " + CommMouve);

                                    // START***SETTING THE VALUES IN LOCAL TABLE

                                    ComCrDrRec.setAccountId(AccNo);
                                    TransactionRefClass param = new TransactionRefClass();
                                    param.setTransactionRef(StmtEntId);
                                    param.setAmount(AmtLcy.toString());
                                    param.setDate(TxnValDate);
                                    param.setDebCre("DR");
                                    ComCrDrRec.addTransactionRef(param);
                                    System.out.println("ComCrDrRec = " + ComCrDrRec);

                                    // END***SETTING THE VALUES IN LOCAL TABLE

                                }
                            }

                        }

                        if (CommMouve.equalsIgnoreCase("default") && !TotTxnAmt.equals(0.0)) {
                            System.out.println("Setting the values for default ");
                            ComCrDrRec.setTotcomcr(TotTxnCrAmt.toString());
                            ComCrDrRec.setTotcomcr(TotTxnDrAmt.toString());
                            TotChgTxnAmt = TotTxnAmt;
                        } else if (CommMouve.equalsIgnoreCase("credit") && !TotCrAmt.equals(0.0)) {
                            System.out.println("Setting the values for Credit ");
                            System.out.println("TotChgTxnAmt = " + TotChgTxnAmt);
                            TotChgTxnAmt = TotCrAmt;
                            ComCrDrRec.setTotcomcr(TotCrAmt.toString());
                        } else if (CommMouve.equalsIgnoreCase("DR ONLY") && !TotDrAmt.equals(0.0)) {
                            System.out.println("Setting the values for debit ");
                            System.out.println("TotChgTxnAmt = " + TotChgTxnAmt);
                            TotChgTxnAmt = TotDrAmt;
                            ComCrDrRec.setTotcomdr(TotDrAmt.toString());
                        }

//                        EbLegacyCommissionAmtRecord legacy = new EbLegacyCommissionAmtRecord();
//                        EbLegacyCommissionAmtTable legacyTable = new EbLegacyCommissionAmtTable(this);
//                        Double LegacyMouvAmt = 0.0;
//                        try {
//                            legacy = new EbLegacyCommissionAmtRecord(
//                                    getDataAccessObject().getRecord("EB.LEGACY.COMMISSION.AMT", arrangementId));
//                            String DeMouvStatus = legacy.getCommCollect().getValue();
//                            if (!DeMouvStatus.equals("YES")) {
//                                LegacyMouvAmt = Double.parseDouble(legacy.getAmount().getValue().toString());
//                            }
//
//                        } catch (Exception e) {
//                            System.out.println(e);
//                        }

                        if (!TotChgTxnAmt.equals(0.0)) {
//                            TotChgTxnAmt += LegacyMouvAmt;
                            NewChgAmt = (double) Math
                                    .round(Math.abs((TotChgTxnAmt) * ((Double.parseDouble(ChRate)) / 100)));
                            System.out.println("NewChgAmt = " + NewChgAmt);
                            ComCrDrRec.setTotalcommouve(NewChgAmt.toString());
                        }

                        try {
                            if (!NewChgAmt.equals(0.0)) {
                                System.out.println("Write the file ComCrDrRec = " + ComCrDrRec);
                                System.out.println("SetId = " + SetId);
                                ComCrDrTab.write(SetId, ComCrDrRec);
                            }
                        } catch (Exception e) {
                        }
                    }

                } catch (Exception e) {
                }

                break;

            case "BBCICOMDECOU":

                // OK
                EbCommHighestOvdTxnRecord CommHiRec = new EbCommHighestOvdTxnRecord();
                EbCommHighestOvdTxnTable CommHiTab = new EbCommHighestOvdTxnTable(this);
                int FromDate = 0;
                int ToDate = 0;
                String SetIdCou = "";
                AaInterestAccrualsRecord AaIntRec = new AaInterestAccrualsRecord();
                Double MaxBal = 0.0;
                List<FromDateClass> FromDateList = null;
                int StartDate = 0;
                int EDate = 0;
                System.out.println("Collect charges for BBCICOMDECOU ");
                // OK

                // OK
                try {
                    AaIntRec = new AaInterestAccrualsRecord(getDataAccessObject().getRecord("AA.INTEREST.ACCRUALS",
                            arrangementId + "-" + "DRINTEREST"));
                    System.out.println("AaIntRec =  " + AaIntRec);
                } catch (Exception e) {
                }

                // START...GET MONTH FROM DATE

                try {
                    System.out.println("case2 line1");
                    LocalDate currentDateCou = LocalDate.parse(adjustPeriodStartDate.toString(), formatter);
                    System.out.println("case2 line2");
                    String MonthCou = String.valueOf(currentDateCou.getMonthValue());
                    System.out.println("case2 line3");
                    String YearCou = String.valueOf(currentDateCou.getYear());
                    System.out.println("case2 line4");

                    // END...GET MONTH FROM DATE

                    // START...SET ID OF LOCAL TABLE

                    SetIdCou = MonthCou + YearCou + "COMDECOUVERT" + AccNo;
                    System.out.println("SetIdCou = " + SetIdCou);

                } catch (Exception e) {
                }

                // END...SET ID OF LOCAL TABLE

                // START...GETTING THE RECORD
                try {
                    CommHiRec = new EbCommHighestOvdTxnRecord(
                            getDataAccessObject().getRecord("EB.COMM.HIGHEST.OVD.TXN", SetIdCou));

                } catch (Exception e) {
                    CommHiRec.setAccountId(AccNo);
                }

            // END...GETTING THE RECORD

            {
                try {
                    CalcTierType = chargePropertyRecord.getCalcTierType();
                    ChRate = CalcTierType.get(0).getChargeRate().getValue().toString();
                    System.out.println("ChRate BBCICOMDECOU= " + ChRate);
                    CommMouve = chargePropertyRecord.getLocalRefField("L.COMMOUVE").getValue().toString();
                    System.out.println("CommMouve BBCICOMDECOU= " + CommMouve);
                } catch (Exception e) {
                }
                
                if (!ChRate.equals("0") && !ChRate.equals("0.00") && !ChRate.equals(""))
//                if (!ChRate.equals("0") && !ChRate.equals("")) // check if
                                                               // charge rate is
                                                               // not equal to 0
                                                               // and not null;
                                                               // // error
                {
                    try {
                        StartDate = Integer.parseInt(adjustPeriodStartDate);
                        EDate = Integer.parseInt(adjustPeriodEndDate);
                        System.out.println("StartDate BBCICOMDECOU= " + StartDate);
                        System.out.println("EDate BBCICOMDECOU= " + EDate);
                    } catch (Exception e) {
                    }

                    if (CategDecouv.contains(Categ)) // check for the valid
                                                     // category in CategDecouv
                                                     // // error
                    {
                        System.out.println("CategDeCouv contains Categ");
                        try {
                            FromDateList = AaIntRec.getFromDate();// error
                            for (int i = 0; i < FromDateList.size(); i++)// checked
                                                                         // from
                            {
                                Double PeriodBalAmt = 0.0;
                                System.out.println("Incrementor I = " + i);
                                FromDate = Integer.parseInt(FromDateList.get(i).getFromDate().getValue().toString());
                                System.out.println("FromDate = " + FromDate);
                                ToDate = Integer.parseInt(FromDateList.get(i).getToDate().getValue().toString());
                                System.out.println("ToDate = " + ToDate);
                                if ((FromDate >= StartDate && ToDate <= EDate)
                                        || (FromDate >= StartDate && FromDate <= EDate)
                                        || (ToDate >= StartDate && ToDate <= EDate)) {
                                    System.out.println("Valid Date");
                                    List<TField> PeriodBalList = FromDateList.get(i).getBalance();
                                    System.out.println("PeriodBalList = " + PeriodBalList);
                                    for (int j = 0; j < PeriodBalList.size(); j++) {
                                        PeriodBalAmt += Double
                                                .parseDouble(FromDateList.get(i).getBalance(j).getValue().toString());
                                        System.out.println("incrementor j = " + j);
                                        System.out.println("PeriodBalAmt = " + PeriodBalAmt);
                                        System.out.println("MaxBal = " + MaxBal);
                                    }

                                    if (PeriodBalAmt > MaxBal) {
                                        System.out.println("PeriodBalAmt >MaxBal");
                                        MaxBal = PeriodBalAmt;
                                        int MaxbalFromDate = FromDate;
                                        int MaxBalToDate = ToDate;
                                        System.out.println("MaxbalFromDate = " + MaxbalFromDate);
                                        System.out.println("MaxBalToDate = " + MaxBalToDate);
                                    }
                                }
                            }

                            if (!MaxBal.equals(0.0)) {
                                System.out.println("Calculate charges for BBCICOMDECOU ");                             
                                NewChgAmt = (double) Math
                                        .round(Math.abs((MaxBal) * ((Double.parseDouble(ChRate)) / 100)));
                                System.out.println("NewChgAmt = " + NewChgAmt);
                                if (!NewChgAmt.equals(0.0) && FromDate != 0 && ToDate != 0) {

                                    CommHiRec.setCategory(Categ);
                                    CommHiRec.setFromDate(String.valueOf(FromDate));
                                    CommHiRec.setToDate(String.valueOf(ToDate));
                                    CommHiRec.setOverdraftAmt(String.valueOf(MaxBal));
                                    CommHiRec.setCommCalc(String.valueOf(NewChgAmt));
                                    System.out.println("CommHiRec = " + CommHiRec);
                                }
                            }
                            try {
                                if (!NewChgAmt.equals(0.0) && FromDate != 0 && ToDate != 0) {
                                    System.out.println("SetIdCou = " + SetIdCou);
                                    CommHiTab.write(SetIdCou, CommHiRec);
                                }
                            } catch (Exception e) {
                            }

                        } catch (Exception e) {
                        }
                    }
                }
            }

                break;
            }

            try {

                if (!NewChgAmt.equals(0.0)) {
                    if ((ChgProp.equals("BBCICOMMOUVE")) || (ChgProp.equals("BBCICOMDECOU"))) {
                        System.out.println("Setting the new charge and reason");
                        System.out.println("adjustChargeAmount = " + (NewChgAmt - Double.parseDouble(chargeAmount)));

                        adjustChargeAmount.set(NewChgAmt - Double.parseDouble(chargeAmount));
                        System.out.println("NewChgAmt = " + NewChgAmt);
                        newChargeAmount.set(NewChgAmt);
                        adjustReason.set("RETAIN.CUSTOMERS.BUSINESS");
                    } else if ((ChgProp.equals("BBCICOMDEPASS"))) {
                        System.out.println("Setting the new charge and reason");
                        System.out.println("adjustChargeAmount = " + (NewChgAmt - Double.parseDouble(chargeAmount)));

                        adjustChargeAmount.set(NewChgAmtComDepass - Double.parseDouble(chargeAmount));
                        System.out.println("NewChgAmtComDepass = " + NewChgAmtComDepass);
                        newChargeAmount.set(NewChgAmtComDepass);
                        adjustReason.set("RETAIN.CUSTOMERS.BUSINESS");
                    }

                }
            } catch (Exception e) {
            }

        } else {
            System.out.println("Skipping EOM COB Charge Processing &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
            // Alternative logic
        }
        

    }

    // function to sort Hashmap by values

    public static HashMap<String, String> sortByValue(HashMap<String, String> hm) {
        LinkedHashMap<String, String> temp = new LinkedHashMap<String, String>();
        try {
            // Create a list from elements of HashMap
            List<Map.Entry<String, String>> list = new LinkedList<Map.Entry<String, String>>(hm.entrySet());

            // Sort the list
            Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
                public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2)

                {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
            });

            // put data from sorted list to Hashmap
            temp = new LinkedHashMap<String, String>();
            for (Map.Entry<String, String> aa : list) {
                temp.put(aa.getKey(), aa.getValue());
            }

        } catch (Exception e) {
        }
        return temp;
    }

    public static Map<String, LocalDate> getFirstAndLastDateOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();

        Map<String, LocalDate> result = new HashMap<>();
        result.put("firstDate", firstDay);
        result.put("lastDate", lastDay);

        return result;
    }

}
