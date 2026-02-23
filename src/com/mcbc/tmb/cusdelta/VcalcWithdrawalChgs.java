package com.mcbc.tmb.cusdelta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TDate;
import com.temenos.api.TField;
import com.temenos.api.TNumber;
import com.temenos.api.TString;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.Calculation;
import com.temenos.t24.api.party.Account;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aainterestaccruals.AaInterestAccrualsRecord;
import com.temenos.t24.api.records.aainterestaccruals.AccrualAmtClass;
import com.temenos.t24.api.records.aainterestaccruals.FromDateClass;
import com.temenos.t24.api.records.aaprddeschangeproduct.AaPrdDesChangeProductRecord;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.aaprddesinterest.FixedRateClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author Debesh - Calculate Redemption fee jar file Name CSD_TmbCusDelta.jar
 *         Routine : TMB.DEPOSIT.WITHDRAW.API
 */
public class VcalcWithdrawalChgs extends Calculation {

    AaInterestAccrualsRecord accrualRec;
    AaBillDetailsRecord billrec;
    boolean debugg = true;
    DataAccess da = new DataAccess(this);
    List<String> FromDtList = new ArrayList<String>();
    List<String> ToDtList = new ArrayList<String>();
    List<String> AccrAmtList = new ArrayList<String>();
    List<String> noDaysList = new ArrayList<String>();
    List<AccrualAmtClass> accamtclass;
    TField nodaystf;
    TField todate;
    TField fromdate;
    TField AccAmt;
    BigDecimal TotIntAmt;
    private BigDecimal TotChgAmt = BigDecimal.ZERO;
    BigDecimal FinalTotalPaidAmt = BigDecimal.ZERO;
    BigDecimal MFactor = BigDecimal.ZERO;
    String adjustAmt = "";
    boolean updFlg = false;
    String PaidAmt;
    String RolloVerDate;
    String CurrDate;
    String EndAccDate;
    String EffectiveDt;
    String LastAccrualdate;
    BigDecimal LastAccruedAmt = BigDecimal.ZERO;
    String ArrStartDate;
    int DecimalPlaces = 13;
    String varrangementId;
    Boolean AfterRenewal = true;
    List<LinkedApplClass> ApplClass;
    String linkAcctId;
    String IntRate;
    boolean manualCalc = false;

    @Override
    public void calculateAdjustedCharge(String arrangementId, String arrangementCcy, TDate adjustEffectiveDate,
            String adjustChargeProperty, String chargeType, AaPrdDesChargeRecord chargePropertyRecord,
            TNumber adjustBaseAmount, String adjustPeriodStartDate, String adjustPeriodEndDate, String sourceActivity,
            String chargeAmount, String activityId, TNumber adjustChargeAmount, TNumber newChargeAmount,
            TString adjustReason, AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, TStructure productPropertyRecord,
            AaProductCatalogRecord productRecord, TStructure record, AaArrangementActivityRecord masterActivityRecord) {
        varrangementId = arrangementId;

        Contract convar = new Contract(this);
        convar.setContractId(arrangementId);

        TStructure renewalCondStructre = convar.getConditionForProperty("RENEWAL");
        AaPrdDesChangeProductRecord renewalRecord = new AaPrdDesChangeProductRecord(renewalCondStructre);
        System.out.println("Renwal Record " + renewalRecord);

        TStructure InterestCondStructre = convar.getConditionForProperty("DEPOSITINT");
        AaPrdDesInterestRecord interestRecord = new AaPrdDesInterestRecord(InterestCondStructre);
        System.out.println("Interest Record " + interestRecord);

        String RenewalDt = renewalRecord.getIdComp3().toString();
        List<FixedRateClass> DepRateClass = interestRecord.getFixedRate();
        for (FixedRateClass DepRateList : DepRateClass) {
            IntRate = DepRateList.getEffectiveRate().toString();
        }

        RolloVerDate = RenewalDt.substring(0, 8);
        LastAccrualdate = "";
        if (debugg)
            System.out.println("Inside AACharge Routine ");
        if (debugg)
            System.out.println("Initial Charge Amount passed " + chargeAmount);

        EffectiveDt = adjustEffectiveDate.toString();
        ArrStartDate = arrangementRecord.getStartDate().toString();
        ApplClass = arrangementRecord.getLinkedAppl();

        if (debugg)
            System.out.println("EffectiveDt " + EffectiveDt);
        if (debugg)
            System.out.println("ArrStartDate " + ArrStartDate);

        try {

            BigDecimal TotIntAmt = new BigDecimal(5);
            linkAcctId = arrangementContext.getLinkedAccount();
            if (debugg)
                System.out.println("Arrangement Activity Record " + arrangementActivityRecord);

            String arrangement = arrangementActivityRecord.getArrangement().toString();
            if (debugg)
                System.out.println("Found the arrangemnt  " + arrangement);

            String AccrualId = varrangementId + "-" + "DEPOSITINT";
            accrualRec = new AaInterestAccrualsRecord(da.getRecord("AA.INTEREST.ACCRUALS", AccrualId));

            if (debugg)
                System.out.println("Accrual Record " + accrualRec);
            List<FromDateClass> FromDate = accrualRec.getFromDate();
            // RolloVerDate = getRollOverDate(varrangementId);
            int intRolloVerDate = Integer.parseInt(RolloVerDate);
            int intEffectiveDate = Integer.parseInt(EffectiveDt);
            int intArrStartDate = Integer.parseInt(ArrStartDate);

            if (intRolloVerDate == 0)
                intRolloVerDate = intArrStartDate;

            if (debugg)
                System.out.println("RolloVerDate " + RolloVerDate);

            for (FromDateClass frdtfld : FromDate) {

                accamtclass = frdtfld.getAccrualAmt();
                nodaystf = frdtfld.getDays();
                todate = frdtfld.getToDate();
                fromdate = frdtfld.getFromDate();
                CurrDate = fromdate.toString();
                EndAccDate = todate.toString();

                int intCurrDate = Integer.parseInt(CurrDate);

                if (intCurrDate >= intRolloVerDate) {
                    AfterRenewal = true;
                } else {
                    AfterRenewal = false;
                }
                if (debugg)
                    System.out.println("nodays  " + nodaystf.toString());
                if (debugg)
                    System.out.println("todate  " + todate);
                TotIntAmt = BigDecimal.ZERO;
                for (AccrualAmtClass Amtindiv : accamtclass) {
                    if (debugg)
                        System.out.println("Inside2 AccrualAmt Loop");

                    TField AccAmt = Amtindiv.getAccrualAmt();
                    TField AcctAmt = Amtindiv.getActAccAmt();

                    AccrAmtList.add(AccAmt.toString());
                    if (debugg)
                        System.out.println("Indiv Amount inside Loop" + AccAmt.toString());

                    String SAccrualAmt = AccAmt.toString();
                    String SActccrualAmt = AcctAmt.toString();
                    if (debugg)
                        System.out.println("Interst Amount read and converetd to string " + SAccrualAmt);
                    if (debugg)
                        System.out.println("Actual Interst Amount " + SActccrualAmt);

                    BigDecimal BChgAmt = new BigDecimal(SAccrualAmt);
                    BigDecimal BActChgAmt = new BigDecimal(SActccrualAmt);

                    if (LastAccrualdate.isEmpty()) {
                        LastAccrualdate = EndAccDate;
                        BigDecimal NoofDays = new BigDecimal(nodaystf.toString());
                        if (debugg)
                            System.out.println("NoofDays in BG " + NoofDays);

                        MFactor = BActChgAmt.divide(NoofDays, DecimalPlaces, BigDecimal.ROUND_HALF_EVEN);
                        if (debugg)
                            System.out.println("MFactor " + MFactor);
                    }

                    if (AfterRenewal) {
                        TotIntAmt = TotIntAmt.add(BChgAmt);
                        if (debugg)
                            System.out.println("Before Rollover Date so selecting this amt " + TotIntAmt);
                    }

                    if (debugg)
                        System.out.println("Tot Interest Amount inside loop " + TotIntAmt);
                    BChgAmt = BigDecimal.ZERO;
                }

                TotChgAmt = TotChgAmt.add(TotIntAmt);
                TotIntAmt = BigDecimal.ZERO;

            }

            if (debugg)
                System.out.println("Tot Charge Amount outloop " + TotChgAmt);

            // If no accrual has happened then Last Accrual Date will be empty
            // manually calculate the Accrual Amount

            if (MFactor.equals(BigDecimal.ZERO)) {
                manualCalc = true;
            }

            if (manualCalc) {
                TotChgAmt = getCalcAccAmt();
                if (debugg)
                    System.out.println("Tot Charge Amount manual calc " + TotChgAmt);
            } else {

                if (LastAccrualdate.isEmpty()) {
                    LastAccrualdate = EndAccDate;
                }
                intEffectiveDate = Integer.parseInt(EffectiveDt);
                if (intEffectiveDate > intRolloVerDate) {
                    AfterRenewal = true;
                }

                if (AfterRenewal) {
                    LastAccruedAmt = CalculateLastAccrued();
                    TotChgAmt = TotChgAmt.add(LastAccruedAmt);
                }
            }

            if (debugg)
                System.out.println("Converting the Charge to calculated " + TotChgAmt);
            chargeAmount = TotChgAmt.toString();
            PaidAmt = GetPaidAmtCollectback(arrangement);
            System.out.println("Total Paid Amount to be deducted " + PaidAmt);
            BigDecimal FinalCharge = new BigDecimal(chargeAmount);

            // BigDecimal FinalPaid = new BigDecimal(PaidAmt);
            // BigDecimal TotalCollect = FinalCharge.subtract(FinalPaid);
            BigDecimal TotalCollect = FinalCharge;
            BigDecimal IntermedAmt = BigDecimal.ZERO;
            IntermedAmt = TotalCollect.setScale(2, RoundingMode.HALF_UP);

            chargeAmount = IntermedAmt.toString();
            System.out.println("Final Charge Amount Passed to the routine " + chargeAmount);

            Account acBal = new Account(this);
            String chargeAmtLoc = getAccountRecDets(linkAcctId, acBal, chargeAmount);
            if (updFlg) {
                adjustChargeAmount.set(adjustAmt);
                newChargeAmount.set(chargeAmtLoc);
                adjustReason.set("Interest Paid Charged");
                if (debugg)
                    System.out
                            .println("Final Details ******" + adjustAmt + "@@@" + chargeAmtLoc + "&&&&" + adjustReason);
            } else {
                adjustChargeAmount.set("0");
                newChargeAmount.set(chargeAmount);
                adjustReason.set("No adjustment ");
                if (debugg)
                    System.out.println("FIRST ELSE" + varrangementId + "-" + chargeAmount);
            }

        } catch (Exception e) {

            System.out.println("Error while updating the charge amount" + arrangementContext.getArrangementId() + " - "
                    + e.getMessage());
            System.out.println("Actual error " + e);
        }

    }

    /**
     * @return
     */
    private BigDecimal getCalcAccAmt() {
        // Manually Calculate the Accrued Interest
        try {
            String Totdays = "365";
            BigDecimal BTotday = new BigDecimal(Totdays);

            Account acBal = new Account(this);
            acBal.setAccountId(linkAcctId);
            System.out.println("acBal Manual local " + acBal);
            BigDecimal wrkBal = new BigDecimal(acBal.getAvailableBalance().getAmount().getValue().toString());
            BigDecimal PerdayBal = wrkBal.divide(BTotday, DecimalPlaces, BigDecimal.ROUND_HALF_EVEN);
            if (debugg)
                System.out.println("wrkBal local " + wrkBal);
            T24Date TD = new T24Date();
            TD.setFirstDate(ArrStartDate);
            TD.setEndDate(EffectiveDt);
            TD.getDiffdays();

            Long DaysNo = TD.getNoDays();
            BigDecimal Rate = new BigDecimal(IntRate);
            BigDecimal Bnod = BigDecimal.valueOf(DaysNo);
            BigDecimal oneHundred = new BigDecimal(100);

            if (debugg)
                System.out.println("No of Days " + DaysNo);
            BigDecimal RatePcnt = Rate.divide(oneHundred);
            BigDecimal BRateCalc = RatePcnt.multiply(Bnod);
            if (debugg)
                System.out.println("CalculatedRate " + BRateCalc);

            BigDecimal IntermedAmt = PerdayBal.multiply(BRateCalc);
            LastAccruedAmt = IntermedAmt.setScale(2, RoundingMode.HALF_UP);
            if (debugg)
                System.out.println("LastAccruedAmt " + LastAccruedAmt);
        } catch (Exception e) {
            System.out.println("Error calculating Manual " + e);
        }
        return LastAccruedAmt;

    }

    /**
     * @return LastAccruedAmt
     */
    private BigDecimal CalculateLastAccrued() {
        // TODO Auto-generated method stub
        BigDecimal LastAccruedAmt = BigDecimal.ZERO;
        T24Date TD = new T24Date();
        TD.setFirstDate(LastAccrualdate);
        TD.setEndDate(EffectiveDt);
        TD.getDiffdays();

        Long DaysNo = TD.getNoDays() - 1;

        if (debugg)
            System.out.println("No of Days " + DaysNo);
        BigDecimal BNOD = BigDecimal.valueOf(DaysNo);
        LastAccruedAmt = MFactor.multiply(BNOD);
        if (debugg)
            System.out.println("LastAccruedAmt " + LastAccruedAmt);
        return LastAccruedAmt;
    }

    /**
     * @param arrangementId
     * @return RolloverDate
     * 
     *         private String getRollOverDate(String zarrangementId) { // TODO
     *         Auto-generated method stub List<String> SortDates = new
     *         ArrayList<>(); try { zarrangementId = zarrangementId.trim();
     *         List<String> recidsAccount = da.selectRecords("",
     *         "AA.ARR.CHANGE.PRODUCT", "", "WITH @ID LIKE " + zarrangementId
     *         +"..."); // "WITH @ID LIKE " + zarrangementId + "-RENEWAL-...
     *         BY-DSND @ID"); String cmd = "WITH @ID LIKE " + zarrangementId +
     *         "-RENEWAL-... BY-DSND @ID"; System.out.println(cmd);
     *         System.out.println("Renewal Dates " + recidsAccount); for (String
     *         indivDate : recidsAccount) { String[] RenwalDtId =
     *         indivDate.split("-"); String RenDate1 = RenwalDtId[2]; if
     *         (debugg) System.out.println("After First split after assignment"
     *         + RenDate1); String yRenewalDate = RenDate1.substring(0, 8);
     *         SortDates.add(yRenewalDate); } SortDates ListDates = new
     *         SortDates(); ListDates.setRollDates(SortDates);
     *         ListDates.GetLatestDates(); ListDates.getMaxDate(); RolloVerDate
     *         = ListDates.getMaxDate(); } catch (Exception e) {
     * 
     *         System.out.println("Error Reading CHANGE PRODUCT " + e); }
     * 
     *         return RolloVerDate; }
     */

    public String getAccountRecDets(String acId, Account acBal, String chargeAmount) {
        String chargeAmtLoc = "0";
        adjustAmt = "0";

        updFlg = false;
        BigDecimal chgAmt = new BigDecimal(chargeAmount);
        if (debugg)
            System.out.println("ChargeAmount in Function " + chgAmt);
        try {
            acBal.setAccountId(acId);
            System.out.println("acBal local " + acBal);
            BigDecimal wrkBal = new BigDecimal(acBal.getAvailableBalance().getAmount().getValue().toString());
            if (debugg)
                System.out.println("wrkBal local " + wrkBal);
            if (debugg)
                System.out.println("chgAmt " + chgAmt);

            chargeAmtLoc = chargeAmount;
            updFlg = true;
            adjustAmt = wrkBal.toString();
            System.out.println("Default case " + wrkBal);

        } catch (Exception e) {
            System.out.println("Error while fetching the account details" + e.getMessage());
        }
        if (debugg)
            System.out.println("Final wrkBal local ***" + chargeAmtLoc);

        return chargeAmtLoc;
    }

    public String GetPaidAmtCollectback(String zarrangementId) {
        BigDecimal GrpTotalPaidAmt = BigDecimal.ZERO;
        BigDecimal IndivPropPaidAmt = BigDecimal.ZERO;
        String PaymentAmt = "";
        System.out.println("RolloVer Date " + RolloVerDate);
        try {
            List<String> recidsAccount = da.selectRecords("", "AA.BILL.DETAILS", "",
                    "WITH ARRANGEMENT.ID EQ " + zarrangementId + " BY-DSND @ID");

            if (debugg)
                System.out.println("Paid Details Bills " + recidsAccount);
            for (String BillId : recidsAccount) {
                PaymentAmt = "";
                if (debugg)
                    System.out.println("Paid Details Bill ID " + BillId);
                billrec = new AaBillDetailsRecord(da.getRecord("AA.BILL.DETAILS", BillId));
                String PayDate = billrec.getActualPayDate().toString();

                int intCurrDate = Integer.parseInt(PayDate);
                int intRolloVerDate = Integer.parseInt(RolloVerDate);
                // int intArrStartDate = Integer.parseInt(ArrStartDate);
                if (intCurrDate == intRolloVerDate) {
                    System.out.println("Paid out for the previous month before rollover , so collect it back");
                    intRolloVerDate = 0;
                }
                if (debugg)
                    System.out.println("PAyment Date " + intCurrDate);
                List<PropertyClass> PropTypeclass = billrec.getProperty();
                for (PropertyClass PropIndiv : PropTypeclass) {

                    String Propertytype = PropIndiv.getProperty().toString();
                    if (Propertytype.equals("DEPOSITINT")) {

                        PaymentAmt = PropIndiv.getPosPropAmount().toString();

                        BigDecimal BPaidAmt = new BigDecimal(PaymentAmt);
                        if (true) {
                            if (debugg)
                                // System.out.println("After Rollover Date so
                                // selecting this amt " + intCurrDate);
                                IndivPropPaidAmt = IndivPropPaidAmt.add(BPaidAmt);
                            BPaidAmt = BigDecimal.ZERO;
                            if (debugg)
                                System.out.println("Individual Total " + IndivPropPaidAmt);
                        }
                    }

                    GrpTotalPaidAmt = GrpTotalPaidAmt.add(IndivPropPaidAmt);
                    IndivPropPaidAmt = BigDecimal.ZERO;
                    if (debugg)
                        System.out.println("Group Total Paid " + GrpTotalPaidAmt);
                }

                FinalTotalPaidAmt = FinalTotalPaidAmt.add(GrpTotalPaidAmt);
                GrpTotalPaidAmt = BigDecimal.ZERO;
                if (debugg)
                    System.out.println("Final Total Paid Amt " + FinalTotalPaidAmt);
            }
            PaidAmt = FinalTotalPaidAmt.toString();

        } catch (Exception e) {
            System.out.println("Error while fetching the Bill details" + e);
        }

        return PaidAmt;
    }

}
