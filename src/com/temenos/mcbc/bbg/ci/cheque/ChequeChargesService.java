package com.temenos.mcbc.bbg.ci.cheque;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.records.chequeissue.ChequeIssueRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Service to apply Frais de Garde charges on stale cheques.
 * Scans cheque records and applies a fund transfer charge if overdue.
 * 
 * @author shegs
 */
public class ChequeChargesService extends ServiceLifecycle {

    private final DataAccess da = new DataAccess(this);

    private String getTodayDate() {
        return new Date(this).getDates().getToday().getValue();
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> listid = new ArrayList<>();
        try {
            listid = da.selectRecords("", "CHEQUE.ISSUE", "",
                    "WITH CHEQUE.STATUS EQ 75 AND L.STATUS NE YES");
        } catch (Exception e) {
            System.err.println("Error fetching cheque issue records: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Fetched Cheque IDs: " + listid);
        return listid != null ? listid : new ArrayList<>();
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        
        String changeDate = getTodayDate();
        
        System.out.println("Default Change Date is" + changeDate);
        
        System.out.println("Processing Cheque Issue Record: " + id);

        try {
            ChequeIssueRecord chequeRec = new ChequeIssueRecord(da.getRecord("", "CHEQUE.ISSUE", "", id));

            String chequeAcct = chequeRec.getLocalRefField("L.ACCOUNT.NO").toString();
            changeDate = chequeRec.getLocalRefField("L.CHANGE.DATE").toString();
            System.out.println("Change Date is " + changeDate);
            
//            long dateDiff = 0;
            if (changeDate.isEmpty()){
            
                return;
            }
            
            long dateDiff = DateDifferenceCalculator(changeDate);

            if (dateDiff > 44) {
                System.out.println("Initiating FT Charge for Cheque ID: " + id);
                
                // Create new funds transfer record
                FundsTransferRecord FTRecord = new FundsTransferRecord(this);
                SynchronousTransactionData syntxn = new SynchronousTransactionData();

                syntxn.setSourceId("CHQ.CHARGE");
                syntxn.setCompanyId("CI2250001");
                syntxn.setVersionId("FUNDS.TRANSFER,FRAIS.DE.GARDE.BBG.CI");
                syntxn.setFunction("INPUT");
                syntxn.setNumberOfAuthoriser("0");

                FTRecord.setDebitAcctNo(chequeAcct);
                FTRecord.setPaymentDetails("Frais-" + id, 0);

                transactionData.add(syntxn);
                records.add(FTRecord.toStructure());

                System.out.println("FT Charge Added Successfully for Cheque ID: " + id);
            } else {
                System.out.println("Cheque ID: " + id + " is not overdue (Days since change: " + dateDiff + ")");
            }

        } catch (Exception e) {
            System.err.println("Error processing cheque ID: " + id + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public long DateDifferenceCalculator(String inDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate givenDate = LocalDate.parse(inDate, formatter);
            LocalDate today = LocalDate.now();
            return ChronoUnit.DAYS.between(givenDate, today);
        } catch (Exception e) {
            System.err.println("Error calculating date difference for date: " + inDate + " - " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}
