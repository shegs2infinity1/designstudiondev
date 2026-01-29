package com.mcbc.tmb.pymt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 *@author - Debesh
 *@version 1.0
 * * /** * @category EB.API - V.AUTH.TELLER.CHEQUES.TMB
 * 
 * @param Application
 *            - TELLER
 * @param AUTH.ROUTINE
 *            - if it's more than 60 days for cheque issue date in DRC raised
 *            override CQ.60.DAYS.TMB - if it's more than 120days for cheques
 *            issued abroad raise override CQ.120.DAYS.TMB then raise Overrides
 * @param Version
 *            - TELLER,LCY.OPRETCQ.E09.TMB
 * @return none
 *
 */
public class tmbAuthTellerCheque extends RecordLifecycle {
    DataAccess da = new DataAccess(this);
    TellerRecord tellerRec;
    boolean debugg = false;
    String ltctry;
    String lcqIssueDate;
    Date firstDate = null;
    Date secondDate = null;
    long noDays;
    TField LChqIssueDate;
    
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        
        tellerRec = new TellerRecord(currentRecord);
        lcqIssueDate = tellerRec.getLocalRefField("L.CQ.DATE").toString();
        ltctry = tellerRec.getLocalRefField("L.CQ.CTRY").toString();
        LChqIssueDate = tellerRec.getLocalRefField("L.CQ.DATE");
        
        // DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);

        try {
            firstDate = sdf.parse(lcqIssueDate);

            if (debugg)
                System.out.println("Cheq issue Date:" + firstDate);
        } catch (Exception e) {
            System.out.println("LocalDate Parse Exception" + e);
        }

        try {
            Session session = new Session(this);
            String today = session.getCurrentVariable("!TODAY");
            secondDate = sdf.parse(today);

            if (debugg)
                System.out.println("todayDate:" + secondDate);
        } catch (Exception e) {
            System.out.println("Today date exception" + e);
        }

        try {
            long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
            noDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            System.out.println("noofDays:" + noDays);
        } catch (Exception e) {
            System.out.println("Date calculation exception " + e);
        }

        if (ltctry.equals("RDC")) {
            if (noDays > 60) {
                List<String> OverrideMsg = new ArrayList<>();
                OverrideMsg.add("CQ.60.DAYS.TMB");
                //int noMsg = tellerRec.getOverride().size();
                //tellerRec.setOverride("CQ.60.DAYS.TMB", noMsg);
                     
               LChqIssueDate.setOverride(OverrideMsg.toString());             
                if (debugg)
                    System.out.println(OverrideMsg.toString());  
                    System.out.println("Override for 60 days set");
            }

        } else {
            if (noDays > 120) {
               List<String> OverrideMsg = new ArrayList<>();
               OverrideMsg.add("CQ.120.DAYS.TMB");
              // int noMsg = tellerRec.getOverride().size();
               //tellerRec.setOverride("CQ.120.DAYS.TMB", noMsg);
            LChqIssueDate.setOverride(OverrideMsg.toString());
           
                if (debugg)
                    System.out.println(OverrideMsg.toString());  
                    System.out.println("Override for 120 days set");
            }
        }

        currentRecord.set(tellerRec.toStructure());

        return tellerRec.getValidationResponse();
        
       
    }


    

}
