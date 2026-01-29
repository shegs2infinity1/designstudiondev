package com.mcbc.tmb.cu;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.chequeissue.ChequeIssueRecord;
import com.temenos.t24.api.records.chequeregistersupplement.ChequeRegisterSupplementRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.tafj.api.client.impl.T24Context;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

/**
 * EB.API - NOFILE.CHEQUE.STATUS jar file - CSD_apiRtns.jar SS:
 * NOFILE.CHEQUE.STATUS ENQUIRY: NOFILE.API.CHEQUE.STATUS.1.0.0 Optimized by
 * Segun
 */

public class NoFileChequeStatus extends Enquiry {
    private static final Logger logger = Logger.getLogger(NoFileChequeStatus.class.getName());

    static {
        try {
//            FileHandler fileHandler = new FileHandler("/u01/t24appl/r23uat2/tafj/log/NoFileChequeStatus.log", true);
//            fileHandler.setFormatter(new SimpleFormatter());
//            fileHandler.setLevel(Level.OFF);
//            logger.addHandler(fileHandler);
            logger.setLevel(Level.OFF);
//            logger.setUseParentHandlers(false); // Disable default console
                                                // handler
        } catch (Exception e) {
//            Logger.getGlobal().log(Level.SEVERE, "Failed to configure logging for NoFileChequeStatus", e);
        }
    }

    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        logger.info("NoFileChequeStatus.setIds() - Starting cheque status enquiry");
        DataAccess dataAccess = new DataAccess((T24Context) this);
        List<String> chequeRegisterIds = new ArrayList<>();
        List<String> chequeIds = new ArrayList<>();
        List<String> finalArray = new ArrayList<>();
        String fullEnqData = "";
        String status = "";
        String beneficiary = "";
        String accountId = "";
        String amount = "";
        String currency = "";
        String remarks = "";
        String chequeType = "CCNB";
        String chqStartNum = "";
        String chqEndNum = "";

        boolean hasChequeNumber = filterCriteria.stream().anyMatch(fc -> fc.getFieldname().equals("chequeNumber"));
        boolean hasAccountId = filterCriteria.stream().anyMatch(fc -> fc.getFieldname().equals("accountId"));
        logger.info("Filter Criteria Check: Cheque Number Present: " + hasChequeNumber + ", Account ID Present: "
                + hasAccountId);
        if (filterCriteria.isEmpty() || !hasChequeNumber || !hasAccountId) {
            accountId = "CQ-ICL-006";
            status = "One or more Search criteria fields are empty";
            chequeType = "NULL";
            amount = "NULL";
            beneficiary = "NULL";
            currency = "NULL";
            logger.warning("Error: One or more Search criteria fields are empty");
            fullEnqData = String.valueOf(accountId) + "*" + chequeType + "*" + amount + "*" + beneficiary + "*"
                    + currency + "*" + status + "*" + remarks;
            finalArray.add(fullEnqData);
            return finalArray;
        }
        if (hasChequeNumber && hasAccountId) {
            try {
                String chequeId = ((FilterCriteria) filterCriteria.stream()
                        .filter(fc -> fc.getFieldname().equals("chequeNumber")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("chequeNumber not found\nCQ-ICL-005")))
                                .getValue();
                accountId = ((FilterCriteria) filterCriteria.stream()
                        .filter(fc -> fc.getFieldname().equals("accountId")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("accountId not found\nCQ-ICL-005"))).getValue();
                logger.info("Processing Cheque ID: " + chequeId + " for Account ID: " + accountId);

                // Try to read the table directly using
                // chequeType+"."+accountId+"."+chequeId
                String tableKey = chequeType + "." + accountId + "." + chequeId;
                logger.info("Attempting direct table read with key: " + tableKey);

                // Read the record once and store it
                ChequeRegisterSupplementRecord tableRecord = null;
                try {
                    tableRecord = new ChequeRegisterSupplementRecord(
                            dataAccess.getRecord("CHEQUE.REGISTER.SUPPLEMENT", tableKey));

                } catch (Exception e) {
                    logger.warning("Failed to read CHEQUE.REGISTER.SUPPLEMENT record: " + e.getMessage());
                }

                // If table record is null, then run the existing logic
                if (tableRecord == null) {
                    logger.info("Direct table read returned null/empty, falling back to selectRecords approach");
                    chequeIds = dataAccess.selectRecords("", "CHEQUE.ISSUE", "",
                            "WITH @ID LIKE CCNB." + accountId + "...");
                    logger.info("Found " + chequeIds.size() + " CHEQUE.ISSUE records for account: " + accountId);
                    boolean chequeExists = false;
                    int chequeIdInt = Integer.parseInt(chequeId);
                    for (String chequeIssueId : chequeIds) {
                        ChequeIssueRecord chequeIssueRecord = new ChequeIssueRecord(
                                dataAccess.getRecord("CHEQUE.ISSUE", chequeIssueId));
                        if (!chequeIssueRecord.getLocalRefField("L.CHQ.NO.START").getValue().isEmpty()
                                && !chequeIssueRecord.getLocalRefField("L.CHQ.NO.END").getValue().isEmpty()) {
                            chqStartNum = chequeIssueRecord.getLocalRefField("L.CHQ.NO.START").getValue();
                            chqEndNum = chequeIssueRecord.getLocalRefField("L.CHQ.NO.END").getValue();
                        } else {
                            chqStartNum = "0";
                            chqEndNum = "0";
                        }
                        int chqStartNumInt = Integer.parseInt(chqStartNum);
                        int chqEndNumInt = Integer.parseInt(chqEndNum);
                        if (chequeIdInt >= chqStartNumInt && chequeIdInt <= chqEndNumInt) {
                            chequeExists = true;
                            //accountId = chequeIssueId;
                            status = "NOT_USED";
                            //chequeType = "NULL";
                            amount = "NULL";
                            beneficiary = "NULL";
                            currency = "NULL";
                            fullEnqData = String.valueOf(accountId) + "*" + chequeType + "*" + amount + "*"
                                    + beneficiary + "*" + currency + "*" + status + "*" + remarks;
                            finalArray.add(fullEnqData);
                            logger.info(
                                    "Cheque found in range, Status set to: " + status + " for Cheque ID: " + chequeId);
                            break;
                        }
                    }
                    if (!chequeExists) {
                        status = "Cheque serial number does not exist";
                        accountId = "CQ-ICL-005";
                        chequeType = "NULL";
                        amount = "NULL";
                        beneficiary = "NULL";
                        currency = "NULL";
                        fullEnqData = String.valueOf(accountId) + "*" + chequeType + "*" + amount + "*" + beneficiary
                                + "*" + currency + "*" + status + "*" + remarks;
                        finalArray.add(fullEnqData);
                        logger.warning("Cheque serial number does not exist for Cheque ID: " + chequeId);
                    }
                } else {
                    logger.info("Direct table read successful, processing record");
                    amount = tableRecord.getAmount().getValue();
                    accountId = tableRecord.getIdComp2().getValue();
                    chequeType = tableRecord.getIdComp1().getValue();
                    beneficiary = tableRecord.getBeneficiary().getValue();
                    currency = tableRecord.getCurrency().getValue();
                    status = tableRecord.getStatus().getValue();
                    if (tableRecord.getRemarks().size() > 0) {
                        remarks = tableRecord.getRemarks(0).getValue();
                    } else {
                        remarks = "No remarks available";
                    }
                    logger.info("Retrieved Data - Account ID: " + accountId + ", Cheque Type: " + chequeType
                            + ", Amount: " + amount + ", Beneficiary: " + beneficiary + ", Currency: " + currency
                            + ", Status: " + status + ", Remarks: " + remarks);
                    fullEnqData = String.valueOf(accountId) + "*" + chequeType + "*" + amount + "*" + beneficiary + "*"
                            + currency + "*" + status + "*" + remarks;
                    finalArray.add(fullEnqData);
                    logger.info("Final Enquiry Data: " + fullEnqData);
                }
            } catch (Exception e) {
                logger.severe("Error processing cheque enquiry: " + e.getMessage());
                accountId = "CQ-ICL-007";
                status = "System error occurred during processing";
                chequeType = "NULL";
                amount = "NULL";
                beneficiary = "NULL";
                currency = "NULL";
                fullEnqData = String.valueOf(accountId) + "*" + chequeType + "*" + amount + "*" + beneficiary + "*"
                        + currency + "*" + status + "*" + remarks;
                finalArray.add(fullEnqData);
            }
        }
        return finalArray;
    }
}