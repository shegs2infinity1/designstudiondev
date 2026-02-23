package com.mcbc.tmb.ws02;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import java.util.logging.Logger;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.pporderentry.PpOrderEntryRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Consolidates transaction records from live and NAU based on type
 * (internal/external) and reference criteria, and logs processing to a file
 * using java.util.logging.
 * 
 * Author: shegs
 */
public class GetAllTransactions extends Enquiry {

    private static final Logger logger = Logger.getLogger(GetAllTransactions.class.getName());
    private static final String TAFJ_HOME = System.getenv("TAFJ_HOME");
    private static final String LOG_DIRECTORY = (TAFJ_HOME != null ? TAFJ_HOME : "/opt") + "/log";
    private static final String LOG_FILE_PATH = LOG_DIRECTORY + "/getAllTxn" + LocalDate.now() + ".log";
    String paymenttype;
    EbWs02PaymentDetailsTmbRecord ebwsO2PayRec;
    DataAccess da = new DataAccess(this);
    static {
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, 5_000_000, 10, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.OFF);
        } catch (Exception e) {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);
            logger.log(Level.SEVERE, "Failed to initialize file logger, using console", e);
        }
    }

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        logger.info("Start processing GetAllTransactions Enquiry.");

        
        List<String> finalArray = new ArrayList<>();
        String type = extractType(filterCriteria);
        String reference = extractReference(filterCriteria);
        String txntype = extractTxnType(filterCriteria);
        

        logger.fine("Extracted TYPE: " + type);
        logger.fine("Extracted REFERENCE: " + reference);
        logger.fine("Extracted TRANSACTION.TYPE: " + txntype);

        if (type.isEmpty()) {
            logger.warning("TYPE not provided in filter criteria. Returning empty result.");
            return finalArray;
        }

        if ("internal".equalsIgnoreCase(type)) {
            processInternalTransactions(da, reference, txntype, finalArray);
        } else if ("external".equalsIgnoreCase(type)) {
            processExternalTransactions(da, reference, finalArray);
        } else {
            logger.warning("Invalid TYPE provided: " + type + ". Must be 'internal' or 'external'.");
        }

        logger.info("Total records processed: " + finalArray.size());
        logger.info("End processing GetAllTransactions Enquiry.");

        return finalArray;
    }

    private void processInternalTransactions(DataAccess da, String reference, String txntype, List<String> finalArray) {
        logger.info("Processing internal transactions for reference: " + reference + ", txnType: " + txntype);

        String selQuery;
        if (reference == null || reference.trim().isEmpty()) {
            selQuery = "WITH CREDIT.THEIR.REF NE ''";
        } else {
            selQuery = "WITH CREDIT.THEIR.REF EQ " + reference;
        }
        if (txntype != null && !txntype.trim().isEmpty()) {
            selQuery += " AND WITH TRANSACTION.TYPE EQ " + txntype;
        }

        // Query live FUNDS.TRANSFER table
        List<String> liveFT = da.selectRecords("", "FUNDS.TRANSFER", "", selQuery);
        logger.info("Retrieved " + liveFT.size() + " internal records from live FUNDS.TRANSFER.");

        // Query NAU FUNDS.TRANSFER table
        List<String> nauFT = da.selectRecords("", "FUNDS.TRANSFER", "$NAU", selQuery);
        logger.info("Retrieved " + nauFT.size() + " internal records from $NAU FUNDS.TRANSFER.");

        processFTRecords(liveFT, "", da, finalArray, "internal");
        processFTRecords(nauFT, "$NAU", da, finalArray, "internal");
    }

    private void processExternalTransactions(DataAccess da, String reference, List<String> finalArray) {
        logger.info("Processing external transactions for reference: " + reference);

        String selQuery;
        if (reference == null || reference.trim().isEmpty()) {
            selQuery = "WITH L.UNIQUECODE NE ''";
        } else {
            selQuery = "WITH L.UNIQUECODE EQ " + reference;
        }

        // Query live PP.ORDER.ENTRY table
        List<String> livePP = da.selectRecords("", "PP.ORDER.ENTRY", "", selQuery);
        logger.info("Retrieved " + livePP.size() + " external records from live PP.ORDER.ENTRY.");

        // Query NAU PP.ORDER.ENTRY table
        List<String> nauPP = da.selectRecords("", "PP.ORDER.ENTRY", "$NAU", selQuery);
        logger.info("Retrieved " + nauPP.size() + " external records from $NAU PP.ORDER.ENTRY.");

        processPPRecords(livePP, "", da, finalArray, "external");
        processPPRecords(nauPP, "$NAU", da, finalArray, "external");
    }

    private void processFTRecords(List<String> records, String version, DataAccess da, List<String> finalArray,
            String type) {
        for (String ftid : records) {
            try {
                logger.fine("Processing FT record: " + ftid + " from version: " + version);

                FundsTransferRecord ftRec = new FundsTransferRecord(da.getRecord("FUNDS.TRANSFER" + version, ftid));
                StringBuilder paymentDet = new StringBuilder();
                StringBuilder override = new StringBuilder();

                String recid = ftid;
                String creditReference = safeToString(ftRec.getCreditTheirRef());
                String creditAcctName = " ";

                for (int i = 0; i < ftRec.getPaymentDetails().size(); i++) {
                    paymentDet.append(ftRec.getPaymentDetails(i).toString()).append(" ");
                }

                String debitAmount = safeToString(ftRec.getDebitAmount());
                String creditAcct = safeToString(ftRec.getCreditAcctNo());
                String debitAcct = safeToString(ftRec.getDebitAcctNo());
                String debitccy = safeToString(ftRec.getDebitCurrency());
                String Ptype = getObdxType(creditReference);

                for (int i = 0; i < ftRec.getOverride().size(); i++) {
                    override.append(ftRec.getOverride(i).toString()).append(" ");
                }

                String recStatus = safeToString(ftRec.getRecordStatus());
                if ("OBDX".equalsIgnoreCase(Ptype)){
                String output = String.join("*", recid, type, creditReference, creditAcctName,
                        paymentDet.toString().trim(), debitAmount, creditAcct, debitAcct, override.toString().trim(),
                        recStatus, debitccy);

                finalArray.add(output);
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing FT record: " + ftid + " from version: " + version, e);
            }
        }
    }

    private void processPPRecords(List<String> records, String version, DataAccess da, List<String> finalArray,
            String type) {
        for (String ppid : records) {
            try {
                logger.fine("Processing PP record: " + ppid + " from version: " + version);

                PpOrderEntryRecord ppRec = new PpOrderEntryRecord(da.getRecord("PP.ORDER.ENTRY" + version, ppid));
                StringBuilder paymentDet = new StringBuilder();
                StringBuilder override = new StringBuilder();
                
                String recid = ppid;
                String uniqueCode = safeLocalRef(ppRec, "L.UNIQUECODE");
                String creditAcctName = " ";

                // Build payment details from PP record
                String beneficiaryName = safeToString(ppRec.getBeneficiaryname());
                if (!beneficiaryName.isEmpty()) {
                    paymentDet.append(beneficiaryName).append(" ");
                }

                String debitAmount = safeToString(ppRec.getTransactionamount());
                String creditAcct = safeToString(ppRec.getBeneficiaryaccount());
                String debitAcct = safeToString(ppRec.getDebitaccountnumber());
                String debitccy = safeToString(ppRec.getTransactioncurrency());

                for (int i = 0; i < ppRec.getOverride().size(); i++) {
                    override.append(ppRec.getOverride(i).toString()).append(" ");
                }

                String recStatus = safeToString(ppRec.getRecordStatus());
                String Ptype = getObdxType(uniqueCode);
                if ("OBDX".equalsIgnoreCase(Ptype)){
                String output = String.join("*", recid, type, uniqueCode, creditAcctName, paymentDet.toString().trim(),
                        debitAmount, creditAcct, debitAcct, override.toString().trim(), recStatus, debitccy);
                 
                finalArray.add(output);
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing PP record: " + ppid + " from version: " + version, e);
            }
        }
    }

    private String extractType(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria fc : filterCriteria) {
            if ("TYPE".equalsIgnoreCase(fc.getFieldname())) {
                return fc.getValue();
            }
        }
        return "";
    }

    private String extractTxnType(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria fc : filterCriteria) {
            if ("TRANSACTION.TYPE".equalsIgnoreCase(fc.getFieldname())) {
                return fc.getValue();
            }
        }
        return "";
    }

    private String extractReference(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria fc : filterCriteria) {
            if ("REFERENCE".equalsIgnoreCase(fc.getFieldname())) {
                return fc.getValue();
            }
        }
        return "";
    }

    private String safeLocalRef(Object rec, String field) {
        try {
            if (rec instanceof FundsTransferRecord) {
                FundsTransferRecord ftRec = (FundsTransferRecord) rec;
                return ftRec.getLocalRefField(field) != null ? ftRec.getLocalRefField(field).toString() : "";
            } else if (rec instanceof PpOrderEntryRecord) {
                PpOrderEntryRecord ppRec = (PpOrderEntryRecord) rec;
                return ppRec.getLocalRefField(field) != null ? ppRec.getLocalRefField(field).toString() : "";
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Missing or invalid local field: " + field, e);
        }
        return "";
    }

    private String safeToString(Object obj) {
        return obj != null ? obj.toString() : "";
    }
    
    private String getObdxType(String obdxid){
        
        paymenttype = "";
        try {
            ebwsO2PayRec = new EbWs02PaymentDetailsTmbRecord(da.getRecord("EB.WS02.PAYMENT.DETAILS.TMB", obdxid));
            paymenttype = ebwsO2PayRec.getInterfaceType().toString();
        } catch (Exception e) {

        }
        return paymenttype;
    }
}