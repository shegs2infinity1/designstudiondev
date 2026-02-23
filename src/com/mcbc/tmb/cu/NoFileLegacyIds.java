package com.mcbc.tmb.cu;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.alternateaccount.AlternateAccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Added for Objects.isNull() and Objects.nonNull()

/**
 * Temenos T24 Enquiry hook to retrieve and map IDs (Account, Customer, Branch)
 * between T24 and a legacy system.
 * This class extends the Enquiry base class and overrides the setIds method.
 */
public class NoFileLegacyIds extends Enquiry {

    /**
     * Processes filter criteria to retrieve T24 and legacy IDs.
     * The method expects a single filter criterion (accountId, customerId, or branchId).
     *
     * @param filterCriteria A list of FilterCriteria objects containing the search parameters.
     * @param enquiryContext The context of the enquiry.
     * @return A list of strings, where each string contains the T24 ID and its
     * corresponding legacy ID separated by an asterisk (e.g., "T24ID*LEGACYID").
     * Returns an error message if criteria are empty, invalid, or no records are found.
     */
    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> resultIds = new ArrayList<>();
        DataAccess dataAccess = new DataAccess(this);

        if (filterCriteria.isEmpty()) {
            resultIds.add("Search criteria cannot be empty.");
            return resultIds;
        }

        // Convert filterCriteria list to a more accessible map
        Map<String, String> criteriaMap = new HashMap<>();
        for (FilterCriteria fc : filterCriteria) {
            criteriaMap.put(fc.getFieldname(), fc.getValue());
        }

        String matchedCaseField = determineMatchedCase(criteriaMap);

        if (Objects.nonNull(matchedCaseField)) {
            String queryId = criteriaMap.get(matchedCaseField);
            List<String> selectRecords = new ArrayList<>();
            String recordType = null; // Used for account and customer to distinguish T24 vs Legacy

            System.out.println("Processing: " + matchedCaseField + " with value: " + queryId);

            switch (matchedCaseField) {
                case "accountId":
                    try {
                        // Attempt to find in ALTERNATE.ACCOUNT (legacy) first
                        AlternateAccountRecord altAccRec = new AlternateAccountRecord(dataAccess.getRecord("ALTERNATE.ACCOUNT", queryId));
                        if (Objects.nonNull(altAccRec)) {
                            selectRecords.add(queryId);
                            recordType = "LEGACY";
                        }
                    } catch (Exception e) {
                        // If not found in ALTERNATE.ACCOUNT, try ACCOUNT (T24)
                        try {
                            AccountRecord acctRec = new AccountRecord(dataAccess.getRecord("ACCOUNT", queryId));
                            if (Objects.nonNull(acctRec)) {
                                selectRecords.add(queryId);
                                recordType = "T24";
                            }
                        } catch (Exception ex) {
                            System.out.println("Account ID not found in ALTERNATE.ACCOUNT or ACCOUNT: " + queryId);
                        }
                    }
                    break;

                case "customerId":
                    try {
                        // Attempt to find in CUSTOMER (T24) first
                        CustomerRecord cusRec = new CustomerRecord(dataAccess.getRecord("CUSTOMER", queryId));
                        if (Objects.nonNull(cusRec)) {
                            selectRecords.add(queryId);
                            recordType = "T24";
                        }
                    } catch (Exception e) {
                        // If not found in CUSTOMER, try CUSTOMER.OLD.CUS.ID (legacy)
                        selectRecords = dataAccess.getConcatValues("CUSTOMER.OLD.CUS.ID", queryId);
                        if (!selectRecords.isEmpty()) {
                            recordType = "LEGACY";
                        } else {
                            System.out.println("Customer ID not found in CUSTOMER or CUSTOMER.OLD.CUS.ID: " + queryId);
                        }
                    }
                    break;

                case "branchId":
                    // Search for branch in COMPANY, checking both @ID and L.LGY.BRN.TMB
                    selectRecords = dataAccess.selectRecords("", "COMPANY", "", "WITH L.LGY.BRN.TMB EQ " + queryId + " OR @ID EQ " + queryId);
                    break;

                case "chequeId":
                    // Placeholder for chequeId logic if needed in the future
                    System.out.println("Handling chequeId case for: " + queryId + " (logic not implemented)");
                    break;

                default:
                    // This case should ideally not be reached if determineMatchedCase is robust
                    System.out.println("Unknown matched case field: " + matchedCaseField);
                    resultIds.add("Invalid search criterion provided.");
                    return resultIds;
            }

            if (selectRecords.isEmpty()) {
                System.out.println("No records found for " + matchedCaseField + " with value: " + queryId);
                resultIds.add("No records found for " + matchedCaseField + ": " + queryId);
                return resultIds;
            }

            // Assume the first record is the primary one for processing
            String newId = selectRecords.get(0);
            String legacyId = null;

            System.out.println("Selected Record ID: " + newId);

            switch (matchedCaseField) {
                case "accountId":
                    if ("LEGACY".equals(recordType)) {
                        AlternateAccountRecord altAccRec = new AlternateAccountRecord(dataAccess.getRecord("ALTERNATE.ACCOUNT", queryId));
                        legacyId = queryId; // The queryId itself is the legacy ID for ALTERNATE.ACCOUNT
                        newId = altAccRec.getGlobusAcctNumber().toString(); // T24 account number
                    } else if ("T24".equals(recordType)) {
                        try {
                            AccountRecord accRec = new AccountRecord(dataAccess.getRecord("ACCOUNT", newId));
                            // Attempt to get the alternate account ID (legacy ID)
                            if (!accRec.getAltAcctType().isEmpty()) {
                                legacyId = accRec.getAltAcctType(0).getAltAcctId().getValue();
                            }
                        } catch (Exception e) {
                            System.out.println("Error retrieving legacy ID for T24 Account: " + newId + " - " + e.getMessage());
                            legacyId = null; // No legacy ID found or an error occurred
                        }
                    }
                    resultIds.add(newId + "*" + Objects.toString(legacyId, ""));
                    break;

                case "customerId":
                    if ("LEGACY".equals(recordType)) {
                        // For legacy customer IDs, dataAccess.getConcatValues returns "LEGACYID*T24ID"
                        String[] ids = newId.split("\\*");
                        if (ids.length == 2) {
                            newId = ids[1]; // T24 customer ID
                            legacyId = queryId; // Legacy customer ID
                        } else {
                            System.out.println("Unexpected format for legacy customer ID: " + newId);
                            legacyId = queryId; // Fallback if format is unexpected
                            newId = null; // Indicate T24 ID not found properly
                        }
                    } else if ("T24".equals(recordType)) {
                        CustomerRecord cusRec = new CustomerRecord(dataAccess.getRecord("CUSTOMER", newId));
                        try {
                            // Attempt to get the local reference for the old customer ID
                            legacyId = cusRec.getLocalRefField("OLD.CUS.ID").getValue();
                        } catch (Exception e) {
                            System.out.println("Error retrieving legacy ID for T24 Customer: " + newId + " - " + e.getMessage());
                            legacyId = null;
                        }
                    }
                    resultIds.add(Objects.toString(newId, "") + "*" + Objects.toString(legacyId, ""));
                    break;

                case "branchId":
                    CompanyRecord compRec = new CompanyRecord(dataAccess.getRecord("COMPANY", newId));
                    legacyId = compRec.getLocalRefField("L.LGY.BRN.TMB").getValue();
                    resultIds.add(newId + "*" + Objects.toString(legacyId, ""));
                    break;

                case "chequeId":
                    // If chequeId had logic, it would be processed here
                    resultIds.add("Cheque ID processing not fully implemented for mapping: " + queryId);
                    break;

                default:
                    // Should not be reached given previous checks
                    resultIds.add("An unexpected error occurred during ID mapping.");
                    break;
            }

            System.out.println("Successful retrieval for " + matchedCaseField + ". Result: " + resultIds.get(0));
            return resultIds;
        }

        resultIds.add("Invalid search criteria: Please provide one of accountId, customerId, branchId, or chequeId.");
        System.out.println("Error: Invalid state - either none or multiple cases are true for filter criteria.");
        return resultIds;
    }

    /**
     * Determines which filter criterion is present in the map.
     * @param criteriaMap A map of filter field names to their values.
     * @return The field name that matches one of the expected criteria, or null if none or multiple are present.
     */
    private String determineMatchedCase(Map<String, String> criteriaMap) {
        if (criteriaMap.containsKey("accountId")) {
            return "accountId";
        } else if (criteriaMap.containsKey("customerId")) {
            return "customerId";
        } else if (criteriaMap.containsKey("branchId")) {
            return "branchId";
        } else if (criteriaMap.containsKey("chequeId")) {
            return "chequeId";
        }
        return null; // No valid single criterion found
    }
}