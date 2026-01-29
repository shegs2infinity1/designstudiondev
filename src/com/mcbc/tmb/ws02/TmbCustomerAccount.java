package com.mcbc.tmb.ws02;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.time.LocalDate;

import org.json.JSONObject;
import org.json.JSONException;

import com.temenos.api.TField;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.customeraccount.CustomerAccountRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * T24 Enquiry hook to filter customer accounts based on customer ID.
 * Retrieves account numbers, parses JSON output to extract all fields,
 * concatenates field values with spaces, and sets the concatenated string as filter criteria.
 * EB.API>TMB.CUSTOMER.ACCOUNT
 * @author shegs
 */
public class TmbCustomerAccount extends Enquiry {

    private static final Logger LOGGER = Logger.getLogger(TmbCustomerAccount.class.getName());
    private static final String CUSTOMER_ACCOUNT_TABLE = "CUSTOMER.ACCOUNT";
    private static final String ID_FIELD = "@ID";
    private static final String EQUALS_OPERAND = "EQ";
//    private static final String TAFJ_HOME = System.getenv("TAFJ_HOME");
//    private static final String LOG_DIRECTORY = (TAFJ_HOME != null ? TAFJ_HOME : "/opt") + "/log";
//    private static final String LOG_FILE_PATH = LOG_DIRECTORY + "/tmbCustomerAccount" + LocalDate.now() + ".log";

    static {
        try {
//            FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, 5_000_000, 10, true);
//            fileHandler.setFormatter(new SimpleFormatter());
//            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.OFF);
        } catch (Exception e) {
//            ConsoleHandler consoleHandler = new ConsoleHandler();
//            consoleHandler.setFormatter(new SimpleFormatter());
//            LOGGER.addHandler(consoleHandler);
            LOGGER.setLevel(Level.OFF);
            LOGGER.log(Level.SEVERE, "Failed to initialize file logger, using console", e);
        }
    }

    /**
     * Sets filter criteria for the enquiry based on customer accounts.
     * Retrieves account numbers for the provided customer ID, logs all fields from the record,
     * concatenates JSON field values with spaces, and sets the concatenated string as filter criteria.
     *
     * @param filterCriteria List of initial filter criteria containing the customer ID
     * @param enquiryContext The enquiry context
     * @return Modified list of filter criteria with concatenated values
     */
    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        if (filterCriteria == null || filterCriteria.isEmpty()) {
            LOGGER.warning("Received empty or null filter criteria");
            return filterCriteria;
        }

        DataAccess dataAccess = new DataAccess(this);
        String customerId = filterCriteria.get(0).getValue();

        if (customerId == null || customerId.trim().isEmpty()) {
            LOGGER.warning("Invalid or empty customer ID provided");
            return filterCriteria;
        }

        try {
            CustomerAccountRecord custAcct = new CustomerAccountRecord(
                dataAccess.getRecord(CUSTOMER_ACCOUNT_TABLE, customerId.trim()));

            String accounts = custAcct.getAccountNumber().getValue();
            LOGGER.info("Account number from customer account: " + accounts);

            // Parse the JSON string from custAcct.toString()
            String accountString = custAcct.toString();
            StringBuilder concatenatedValues = new StringBuilder();
            
            try {
                JSONObject jsonObject = new JSONObject(accountString);
                LOGGER.info("Parsing JSON output for customer ID: " + customerId);
                
                // Collect all values and concatenate with space
                jsonObject.keySet().forEach(key -> {
                    String value = jsonObject.getString(key);
                    LOGGER.info("Field: " + key + ", Value: " + value);
                    if (concatenatedValues.length() > 0) {
                        concatenatedValues.append(" ");
                    }
                    concatenatedValues.append(value);
                });
                
                LOGGER.info("Concatenated JSON values: " + concatenatedValues.toString());
            } catch (JSONException e) {
                LOGGER.log(Level.SEVERE, "Failed to parse JSON for customer ID: " + customerId, e);
                concatenatedValues.setLength(0); // Clear concatenatedValues on JSON error
            }

            String criteriaValue = concatenatedValues.length() > 0 ? concatenatedValues.toString().trim() : accounts;
            
            if (criteriaValue == null || criteriaValue.trim().isEmpty()) {
                LOGGER.warning("No valid criteria value found for customer ID: " + customerId);
                filterCriteria.clear();
                return filterCriteria;
            }

            filterCriteria.clear();

            FilterCriteria accountCriteria = new FilterCriteria();
            accountCriteria.setFieldname(ID_FIELD);
            accountCriteria.setOperand(EQUALS_OPERAND);
            accountCriteria.setValue(criteriaValue);
            filterCriteria.add(accountCriteria);

            LOGGER.info("Successfully set filter criteria for customer ID: " + customerId + " with value: " + criteriaValue);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer accounts for ID: " + customerId, e);
            filterCriteria.clear();
            FilterCriteria accountCriteria = new FilterCriteria();
            accountCriteria.setFieldname(ID_FIELD);
            accountCriteria.setOperand(EQUALS_OPERAND);
            accountCriteria.setValue("392");
            filterCriteria.add(accountCriteria);
        }

        return filterCriteria;
    }
}