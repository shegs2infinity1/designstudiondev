package com.mcbc.tmb.obdx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Consolidates FT records from live and NAU based on transaction type
 * and logs processing to a file using java.util.logging.
 * 
 * Author: shegs
 */
public class GetAllFT extends Enquiry {

    private static final Logger logger = Logger.getLogger(GetAllFT.class.getName());
    static {
        try {
            // Resolve the TAFJ home directory from the environment
//            String tafjHome = System.getenv("tafj");
//            if (tafjHome == null || tafjHome.isEmpty()) {
//                throw new IllegalStateException("Environment variable 'tafj' is not set.");
//            }

            // Build the log file path: $tafj/log/GetAllFT.log
//            String logFilePath = tafjHome + "/log/GetAllFT.log";
            String logFilePath = "/u01/t24appl/r23test/tafj/log/GetAllFT.log";
            // Setup the FileHandler for logging
            FileHandler fileHandler = new FileHandler(logFilePath, true); // true = append
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.OFF);

            logger.addHandler(fileHandler);
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false); // Avoid console duplication

        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to initialize file logging for GetAllFT", e);
        } catch (IllegalStateException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "TAFJ log directory not available", e);
        }
    }

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        logger.info("Start processing GetAllFT Enquiry.");

        DataAccess da = new DataAccess(this);
        List<String> finalArray = new ArrayList<>();
        String txntype = extractTxnType(filterCriteria);
        String lbilluser = extractbilluser(filterCriteria);
        String lbillfile = extractbillfile(filterCriteria);

        logger.fine("Extracted TRANSACTION.TYPE: " + txntype);
        logger.fine("Extracted L.BILL.USER: " + lbilluser);

        if (txntype.isEmpty()) {
            logger.warning("No TRANSACTION.TYPE provided in filter criteria. Returning empty result.");
            return finalArray;
        }
        
        String selquery = "WITH TRANSACTION.TYPE EQ " + txntype;
        String selquery1 = "";
        String selquery2 = "";
        if (!lbilluser.isEmpty()) {
             selquery1 = " AND L.BILL.USER EQ " + lbilluser;
        }
        
        if (!lbillfile.isEmpty()) {
            selquery2 = " AND L.AUTOBILL.FILE EQ " + lbillfile;
        }
        
        String FinalQuery = selquery + selquery1 +  selquery2;
        
        List<String> liveFT = da.selectRecords("", "FUNDS.TRANSFER", "", FinalQuery);
        
        List<String> nauFT = da.selectRecords("", "FUNDS.TRANSFER", "$NAU", FinalQuery);
        
        

        logger.info("Retrieved " + liveFT.size() + " records from live.");
//        System.out.println("Retrieved " + liveFT.size() + " records from live.");
        logger.info("Retrieved " + nauFT.size() + " records from $NAU.");
//        System.out.println("Retrieved " + nauFT.size() + " records from $NAU.");

        processFTRecords(liveFT, "", da, finalArray);
        processFTRecords(nauFT, "$NAU", da, finalArray);

        logger.info("Total records processed: " + finalArray.size());
        logger.info("End processing GetAllFT Enquiry.");

        return finalArray;
    }

    private void processFTRecords(List<String> records, String version, DataAccess da, List<String> finalArray) {
        for (String ftid : records) {
            try {
                logger.fine("Processing FT record: " + ftid + " from version: " + version);

                FundsTransferRecord ftRec = new FundsTransferRecord(da.getRecord("FUNDS.TRANSFER" + version, ftid));
                StringBuilder paymentDet = new StringBuilder();
                StringBuilder override = new StringBuilder();

                String recid = ftid;
                String lBillUser = safeLocalRef(ftRec, "L.BILL.USER");
                String lAutobill = safeLocalRef(ftRec, "L.AUTOBILL.FILE");
                String lObdxRefTMB = safeLocalRef(ftRec, "L.OBDX.REF.TMB");
                String creditAcctName = " ";

                for (int i = 0; i < ftRec.getPaymentDetails().size(); i++) {
                    paymentDet.append(ftRec.getPaymentDetails(i).toString()).append(" ");
                }
                
                String debitAmount = safeToString(ftRec.getDebitAmount());
                String creditAcct = safeToString(ftRec.getCreditAcctNo());
                String debitAcct = safeToString(ftRec.getDebitAcctNo());
                String debitccy = safeToString(ftRec.getDebitCurrency());

                for (int i = 0; i < ftRec.getOverride().size(); i++) {
                    override.append(ftRec.getOverride(i).toString()).append(" ");
                }

                String recStatus = safeToString(ftRec.getRecordStatus());

                String output = String.join("*",
                        recid,
                        lBillUser,
                        creditAcctName,
                        paymentDet.toString().trim(),
                        debitAmount,
                        creditAcct,
                        debitAcct,
                        override.toString().trim(),
                        recStatus,
                        debitccy,
                        lAutobill,
                        lObdxRefTMB
                );

                finalArray.add(output);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing FT record: " + ftid + " from version: " + version, e);
            }
        }
    }

    private String extractTxnType(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria fc : filterCriteria) {
            if ("TRANSACTION.TYPE".equalsIgnoreCase(fc.getFieldname())) {
                return fc.getValue();
            }
        }
        return "";
    }
    
    private String extractbilluser(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria fc : filterCriteria) {
            if ("L.BILL.USER".equalsIgnoreCase(fc.getFieldname())) {
                return fc.getValue();
            }
        }
        return "";
    }
    
    private String extractbillfile(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria fc : filterCriteria) {
            if ("L.AUTOBILL.FILE".equalsIgnoreCase(fc.getFieldname())) {
                return fc.getValue();
            }
        }
        return "";
    }

    private String safeLocalRef(FundsTransferRecord rec, String field) {
        try {
            return rec.getLocalRefField(field) != null ? rec.getLocalRefField(field).toString() : "";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Missing or invalid local field: " + field, e);
            return "";
        }
    }

    private String safeToString(Object obj) {
        return obj != null ? obj.toString() : "";
    }
}
