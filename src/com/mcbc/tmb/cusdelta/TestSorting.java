package com.mcbc.tmb.cusdelta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.temenos.api.TField;

/**
 * TODO: Document me!
 *
 * @author debdas
 *
 */
public class TestSorting {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        
        
        
        String FromAmt = "56601000";
        String ToAmt = "115000028";
        String OvrDet = "DUMMY";
        BigDecimal bLocAmt = new BigDecimal("10000");
        String Override = "";
        BigDecimal bfromAmt;
        BigDecimal btoAmt;
        boolean debugg = true;
        bfromAmt = new BigDecimal(FromAmt);
        if (ToAmt.isEmpty()) {
            btoAmt = BigDecimal.ZERO;
        } else {
            btoAmt = new BigDecimal(ToAmt);
        }

        System.out.println("Checking Comparison value blocAmt to be smaller " + bLocAmt.compareTo(bfromAmt));
            if (debugg)
                System.out.println("Checking Override for from Amount " + FromAmt);
            bfromAmt = new BigDecimal(FromAmt);
            if (ToAmt.isEmpty()) {
                btoAmt = BigDecimal.ZERO;
            } else {
                btoAmt = new BigDecimal(ToAmt);
            }
            if (ToAmt.isEmpty()) {
                if (bLocAmt.compareTo(bfromAmt) > 0)
                    
                    Override = OvrDet;
            } else {
                if (bLocAmt.compareTo(bfromAmt) < 0)
                    Override = "";
                if (bLocAmt.compareTo(btoAmt) > 0)
                    Override = OvrDet;
            }
            if (debugg)
                System.out.println("Checking Override status " + Override);
        
        
        
        
        
       String Function1 = "";
       String Function2 = "";
        String firstDate =  "20240701";
        String secondDate = "20240828";
        List<String> PvalueList = new ArrayList<String>();
        PvalueList.add("firstValue");
        PvalueList.add("SecValue");
        
        
        
        int noValues = PvalueList.size();
        System.out.println("noValues " +noValues);
        Function1 = PvalueList.get(0).toString();
        if (noValues > 1) {
            Function2 = PvalueList.get(1).toString();
        }
        
        System.out.println("  String Function1 = ;" + Function1);
        System.out.println("  String Function2 = ;" + Function2);
        
        
        BiFunction<String, String, String> concat = TestSorting::PrintzData;
        String concatenatedResult = concat.apply(firstDate, secondDate);
        System.out.println ("The result is " + concatenatedResult);
        T24Date TD = new T24Date();
        TD.setFirstDate(firstDate);
        TD.setEndDate(secondDate);
        TD.getDiffdays();
        
        Long DaysNo = TD.getNoDays();
        
System.out.println("No of Days " + DaysNo);
        

        
        
        List<String> SortDates = new ArrayList<>();
        try {
            
            List<String> recidsAccount = new ArrayList<>();
            recidsAccount.add("AA24240H26K0-RENEWAL-20240801.01");
            recidsAccount.add("AA24240H26K0-RENEWAL-20240809.01");
            recidsAccount.add("AA24240H26K0-RENEWAL-20240807.01");
            recidsAccount.add("AA24240H26K0-RENEWAL-20240805.01");
            recidsAccount.add("AA24240H26K0-RENEWAL-20240803.01");
            recidsAccount.add("AA24240H26K0-RENEWAL-20240802.01");
            recidsAccount.add("AA24240H26K0-RENEWAL-20240806.01");
            System.out.println(recidsAccount);
            for (String indivDate : recidsAccount )
            {
                String[] RenwalDtId = indivDate.split("-");
                System.out.println("After First split" + RenwalDtId[2]);
                String RenDate1 = RenwalDtId[2];
                System.out.println("After First split after assignment" + RenDate1);
                String yRenewalDate = RenDate1.substring(0,8);
                
                System.out.println("Renewal Date Extarcted " + yRenewalDate );
                SortDates.add(yRenewalDate);       
            }
            
            System.out.println("");
        SortDates ListDates = new SortDates();
        ListDates.setRollDates(SortDates);
        ListDates.GetLatestDates();
        //ListDates.getMaxDate(); 
        String RolloVerDate = ListDates.getMaxDate();
        System.out.println("RolloverDate " + RolloVerDate);
        } catch (Exception e ) {
            
            System.out.println("Error Reading CHANGE PRODUCT " + e);
        }
        

    }

    /**
     * @return
     */
    private static String PrintzData(String s1, String s2) {

        int resul1 = Integer.parseInt(s1) + Integer.parseInt(s2);
        String result = Integer.toString(resul1);

        // TODO Auto-generated method stub
        return result;
    }

}
