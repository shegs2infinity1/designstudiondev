package com.mcbc.tmb.cusdelta;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.temenos.api.TDate;
import com.temenos.api.TNumber;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddessettlement.AaPrdDesSettlementRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebcontractbalances.EbContractBalancesRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author Debesh -Calculates the Safe Deposits Rental on Pro Rata Basis EB.API
 *         - TMB.SAFE.DEPOSIT.API
 * 
 *
 */
public class VcalcSafedepFeesTmb extends ActivityLifecycle {

    boolean debugg = true;
    String ftId;
    String fixedAmountVal;
    String EffDt;
    EbContractBalancesRecord ecbrec;
    AaPrdDesChargeRecord aaChargeRec;
    AaPrdDesSettlementRecord aaSetlRec = null;
    DataAccess da = new DataAccess(this);

    BigDecimal bgFixAmount;
    BigDecimal bgRentalAmt;
    TDate DTToday;
    TDate startDate;
    TDate endDate;
    Double DRentalAmt;

    @Override
    public void defaultFieldValues(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record) {

   //     String arrId = arrangementActivityRecord.getArrangement().toString();
        if (debugg)
            System.out.println("Inside calling routine VcalcSafeDeprentalTmb");
        EffDt = arrangementActivityRecord.getEffectiveDate().getValue();
        String EffY = EffDt.substring(0, 4);
        String EOY = EffY + "12" + "31";

        Session session = new Session(this);
        DatesRecord dateRec = new DatesRecord(da.getRecord("DATES", session.getCompanyId()));
        String todayDate = dateRec.getToday().toString();

        System.out.println("todayDate:" + todayDate);
        DTToday = new TDate(todayDate);
        startDate = new TDate(EffDt);

        endDate = new TDate(EOY);

        Date date = new Date(this);
        TNumber daysDiff = date.getWorkingDayDifference(startDate, endDate);

        try {
          
            aaChargeRec = new AaPrdDesChargeRecord(record);
            if (debugg)
                System.out.println("Property  extracted");
            fixedAmountVal = (aaChargeRec.getFixedAmount().getValue());
            System.out.println("fixedAmountVal " + fixedAmountVal);
            bgFixAmount = new BigDecimal(fixedAmountVal);
            bgRentalAmt = bgFixAmount;
            // DRentalAmt = new Double(fixedAmountVal);
            if (debugg)
                System.out.println("Rental Amount" + fixedAmountVal);

        } catch (Exception econtract) {
            System.out.println("Error creating Contract :" + econtract);
        }

        try {
            int intCurrYr = DTToday.getYear();
            int intEffYr = startDate.getYear();

            if (intCurrYr == intEffYr) {
                System.out.println("daysDiff:" + daysDiff);
                // String totDays = String.valueOf(daysDiff);
                int calcDays = daysDiff.intValue();
                calcDays = Math.abs(calcDays);
                // Double DRentalAmt = new Double(fixedAmountVal);
                int totdays = 365;
                // DRentalAmt = (DRentalAmt / totdays) * calcDays;

                BigDecimal Div = new BigDecimal(totdays);
                BigDecimal bgCalc = new BigDecimal(calcDays);
                System.out.println("No of daysbg " + bgCalc );
                System.out.println("Divbg " + Div);
                bgRentalAmt = (bgFixAmount.multiply(bgCalc)).divide(Div,2,RoundingMode.HALF_DOWN);
System.out.println("CalculatedRentalAmount "+ bgRentalAmt);
                // fixedAmountVal = DRentalAmt.toString();
                fixedAmountVal = bgRentalAmt.toString();
                String s = fixedAmountVal.trim();
                // REMOVE LAST 2 DECIMALS 
                s =  s.substring(0, s.length() - 3);  
                fixedAmountVal= s;
                System.out.println("CalculatedRentalAmount String "+ fixedAmountVal);             
                aaChargeRec.setFixedAmount(fixedAmountVal);
                
                System.out.println("Calculated Rental:" + fixedAmountVal);
            }
        } catch (Exception ConVerr) {
            System.out.println("Conversion Error " + ConVerr);
        }

        record.set(aaChargeRec.toStructure());

    }

}
