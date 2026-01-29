package com.mcbc.bbg.ci.commissions;

import com.temenos.api.TDate;
import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.party.Account;
import com.temenos.t24.api.records.aaarraccount.AaArrAccountRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddescharge.CalcTierTypeClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.stmtentry.StmtEntryRecord;
import com.temenos.t24.api.records.transaction.TransactionRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebcommonparambbgsn.EbCommonParamBbgSnRecord;
import com.temenos.t24.api.tables.ebcommonparambbgsn.ParamNameClass;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */
public class computeCommission extends ServiceLifecycle {
    
    private final DataAccess da = new DataAccess(this);
    
    private String getTodayDate() {
        return new Date(this).getDates().getToday().getValue();
    }
    
    Session currSession = new Session(this); 
    
    DataAccess getDataAccessObject() {
        return new DataAccess(this);
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> listid = new ArrayList<>();
        try {
            listid = da.selectRecords("", "AA.ARRANGEMENT", "",
                    "WITH PRODUCT.GROUP EQ CURRENT.ACCT.ENT.BBGCI OR PRODUCT.GROUP EQ CURRENT.ACCOUNTS.BBGCI");
        } catch (Exception e) {
            System.err.println("Error fetching arrangement records: " + e.getMessage());
            e.printStackTrace();
        }

        return listid != null ? listid : new ArrayList<>();
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        // TODO Auto-generated method stub
        List<ParamNameClass> paramVal;
        EbCommonParamBbgSnRecord comParRec;

        // Category and exclusion lists
        Set<String> categMouv = new HashSet<>();
        Set<String> categDepass = new HashSet<>();
        Set<String> categDecouv = new HashSet<>();
        Set<String> txnsExcMouve = new HashSet<>();
        Set<String> txnsExcDepass = new HashSet<>();
        
        try {
            comParRec = new EbCommonParamBbgSnRecord(da.getRecord("EB.COMMON.PARAM.BBG.SN", "COMMISSION.BBCI"));
            paramVal = comParRec.getParamName();

            for (int i = 0; i < paramVal.size(); i++) {
                String paramName = paramVal.get(i).getParamName().getValue().toString().toUpperCase();
                List<TField> values = comParRec.getParamName(i).getParamValue();

                switch (paramName) {
                    case "CATEGORY.MOUVEMENT":
                        for (TField val : values) categMouv.add(val.getValue().toString());
                        break;
                    case "TXNS.EXCLUDE.MOUVE":
                        for (TField val : values) txnsExcMouve.add(val.getValue().toString());
                        break;
                    case "CATEGORY.DEPASSEMENT":
                        for (TField val : values) categDepass.add(val.getValue().toString());
                        break;
                    case "EXCLUDED.TXN.DEPASSE":
                        for (TField val : values) txnsExcDepass.add(val.getValue().toString());
                        break;
                    case "CATEGORY.DECOUVERT":
                        for (TField val : values) categDecouv.add(val.getValue().toString());
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", id));
            String accNo = arrRec.getLinkedAppl(0).getLinkedApplId().getValue().toString();
            AccountRecord accRec = new AccountRecord(da.getRecord("ACCOUNT", accNo));
            String category = accRec.getCategory().getValue().toString();
            String cocode = arrRec.getCoCodeRec().toString();

            Account ac = new Account(this);
            ac.setAccountId(accNo);
            
            String TodayDate = getTodayDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            boolean eomCob = false;
            int Iyear = 0;
            int Imonth = 0;
            String CoBStatus = null;
            try {

                String CompanyCode = currSession.getCompanyId();
//                String orgSystemDate = arrangementActivityRecord.getOrgSystemDate().toString();
                DatesRecord CobDatesRec = new DatesRecord(getDataAccessObject().getRecord("DATES", CompanyCode + "-COB"));
                DatesRecord DatesRec = new DatesRecord(getDataAccessObject().getRecord("DATES", CompanyCode));
                // System.out.println("Date Rec "+ DatesRec);
                
                CoBStatus = DatesRec.getCoBatchStatus().toString();
                System.out.println("COB Status  " + CoBStatus);
                
                String CobStat = currSession.getOnlineStatus();
                System.out.println("COB Status  " + CobStat);

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
                
//                String Syear = DatesRec.getLastPeriodEnd().toString().substring(0, 4);
//                String Smonth = DatesRec.getLastPeriodEnd().toString().substring(4, 6);
//                
                Iyear = Integer.parseInt(Syear);
                Imonth = Integer.parseInt(Smonth);
                int YYYYMM = Iyear+Imonth;
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
            
            TDate startDate = new TDate(CalendarFirstDay);
            TDate endDate = new TDate(CalendarLastDay);
            
            int StartDate = Integer.parseInt(startDate.toString());
            int EDate = Integer.parseInt(endDate.toString());


//            TDate startDate = new TDate("20251201");
//            TDate endDate = new TDate("20251231");
            List<String> stmtEntries = ac.getEntries("BOOK", "", "", "", startDate, endDate);

            Contract contract = new Contract(this);
            contract.setContractId(id);
            System.out.println(id);
            AaPrdDesChargeRecord chargeRec = new AaPrdDesChargeRecord(contract.getConditionForProperty("BBCICOMMOUVE"));
            String chRateStr = chargeRec.getCalcTierType().get(0).getChargeRate().getValue().toString();
            String commMouve = chargeRec.getLocalRefField("L.COMMOUVE").getValue().toString();

            if (chRateStr.isEmpty() || chRateStr.equals("0")) return;
            double chRate = Double.parseDouble(chRateStr);

            double totalTxnAmt = 0.0, totalCrAmt = 0.0, totalDrAmt = 0.0, totalChgTxnAmt = 0.0;

            for (String stmtId : stmtEntries) {
                StmtEntryRecord stmtEntRec = new StmtEntryRecord(da.getRecord("STMT.ENTRY", stmtId));
                String txnCode = stmtEntRec.getTransactionCode().getValue().toString();
                double amtLcy = Double.parseDouble(stmtEntRec.getAmountLcy().getValue().toString());

                TransactionRecord txnRec = new TransactionRecord(da.getRecord("TRANSACTION", txnCode));
                String initiator = txnRec.getInitiation().getValue().toString();

                if (initiator.equalsIgnoreCase("CUSTOMER") && categMouv.contains(category)
                        && !txnsExcMouve.contains(txnCode)) {
                    switch (commMouve.toLowerCase()) {
                        case "default":
                            totalTxnAmt += Math.abs(amtLcy);
                            break;
                        case "credit":
                            if (amtLcy > 0) totalCrAmt += amtLcy;
                            break;
                        case "dr only":
                            if (amtLcy < 0) totalDrAmt += amtLcy;
                            break;
                    }
                }
            }

            switch (commMouve.toLowerCase()) {
                case "default":
                    totalChgTxnAmt = totalTxnAmt;
                    break;
                case "credit":
                    totalChgTxnAmt = totalCrAmt;
                    break;
                case "dr only":
                    totalChgTxnAmt = totalDrAmt;
                    break;
            }

            double newChgAmt = 0.0;
            if (totalChgTxnAmt != 0.0) {
                newChgAmt = Math.round(Math.abs(totalChgTxnAmt * chRate / 100));
            }

            // Write to file if there’s a charge
            if (newChgAmt != 0.0) {
                writeChargeToFile(id, newChgAmt, totalChgTxnAmt);
                writeofsToFile(id, newChgAmt, cocode);
            }

            System.out.println("Charge Amount for ID " + id + ": " + newChgAmt);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Writes charge details to a file
    private void writeChargeToFile(String id, double chargeAmount, double totalChgTxnAmt) {
        BigDecimal FtotalChgTxnAmt = new BigDecimal(totalChgTxnAmt);
        String filePath = "/t24appl/t24prod/t24/bnk/UD/COM.OUT/202512ComMouvecharges_output.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("ID: " + id + ", Charge: " + chargeAmount + ", TotalTxnAmt: " + FtotalChgTxnAmt);
            writer.newLine();
            System.out.println("Charge written to file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
    private void writeofsToFile(String id, double chargeAmount,String cocode) {
        String filePath = "/t24appl/t24prod/t24/bnk/UD/COM.OUT/202512OFSComMouvcharges_output.txt";
        String ofspost = "AC.CHARGE.REQUEST,INPUT/I/PROCESS//0,INPUTT/Password/"+cocode+",,REQUEST.TYPE=CHARGE,DEBIT.ACCOUNT="+id
                +",CHARGE.CCY=XOF,CHARGE.DATE=20251231,CHARGE.CODE=COMMOUVE,CHARGE.AMOUNT="+chargeAmount
                +",EXTRA.DETAILS=BBCICOMMOUVEDEC2025,STATUS=PAID,RELATED.REF=BBCICOMMOUVE";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(ofspost);
            writer.newLine();
            System.out.println("Charge Ofs written to file.");
        } catch (IOException e) {
            e.printStackTrace();
        } 
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
