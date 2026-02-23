package com.mcbc.tmb.ws02;

import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.time.LocalDate;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Enquiry hook to filter AA arrangements based on either account or customer number.
 * Filters out unauthorized arrangements and maps account/customer to arrangement IDs.
 *
 * @author shegs
 */
public class tmbCasaReadBuild extends Enquiry {
    private static final Logger LOGGER = Logger.getLogger(tmbCasaReadBuild.class.getName());
    private static final String ACCOUNT_FIELD = "LINKED.APPL.ID";
    private static final String CUSTOMER_FIELD = "CUSTOMER";
//    private static final String TAFJ_HOME = System.getenv("TAFJ_HOME");
//    private static final String LOG_DIRECTORY = (TAFJ_HOME != null ? TAFJ_HOME : "/opt") + "/log";
//    private static final String LOG_FILE_PATH = LOG_DIRECTORY + "/tmbCasaReadBuild" + LocalDate.now() + ".log";
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

    static {
        try {
//            FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, true);
//            fileHandler.setFormatter(new SimpleFormatter());
//            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.OFF);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize logger", e);
        }
    }

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        LOGGER.info("Starting setFilterCriteria with input filterCriteria size: " + filterCriteria.size());
        
        LOGGER.info("Starting setFilterCriteria with input filterCriteria size: " + filterCriteria.toString());

        DataAccess da = new DataAccess(this);
        String acctNumber = "";
        String custNumber = "";
        
        // Extract account or customer number from filter criteria
        try {
            for (FilterCriteria criteria : filterCriteria) {
                String fieldName = criteria.getFieldname();
                String value = criteria.getValue();
                if (ACCOUNT_FIELD.equalsIgnoreCase(fieldName) && !value.isEmpty()) {
                    acctNumber = value;
                    LOGGER.fine("Account number retrieved: " + acctNumber);
                } else if (CUSTOMER_FIELD.equalsIgnoreCase(fieldName) && !value.isEmpty()) {
                    custNumber = value;
                    LOGGER.fine("Customer number retrieved: " + custNumber);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error extracting filter criteria values", e);
            return filterCriteria;
        }

        // Clear existing criteria and add filter for authorized arrangements
        FilterCriteria arrStatusCriteria = new FilterCriteria();
        arrStatusCriteria.setFieldname("ARR.STATUS");
        arrStatusCriteria.setOperand("NE");
        arrStatusCriteria.setValue("UNAUTH");
        filterCriteria.clear();
        filterCriteria.add(arrStatusCriteria);
        LOGGER.fine("Added ARR.STATUS filter for authorized arrangements");

        // Process customer number if provided
        if (!custNumber.isEmpty()) {
            try {
                AaCustomerArrangementRecord customerArr = new AaCustomerArrangementRecord(
                    da.getRecord("AA.CUSTOMER.ARRANGEMENT", custNumber));
                List<ProductLineClass> productLines = customerArr.getProductLine();
                
                for (ProductLineClass productLine : productLines) {
                    LOGGER.fine("Product Line is " + productLine.toString());
                    if ("ACCOUNTS".equals(productLine.getProductLine().toString())) {
                        List<ArrangementClass> arrangements = productLine.getArrangement();                   
                        String arrangementIds = arrangements.stream()
                               .map(arr -> String.valueOf(arr.getArrangement()))
                                .collect(Collectors.joining(" "));
                        FilterCriteria custCriteria = new FilterCriteria();
                        custCriteria.setFieldname("@ID");
                        custCriteria.setOperand("EQ");
                        custCriteria.setValue(arrangementIds);
                        filterCriteria.add(custCriteria);
                        LOGGER.fine("Added customer arrangement filter: " + arrangementIds);
                        break;
                    }
                }
            } catch (Exception e) {
                FilterCriteria custCriteria = new FilterCriteria();
                custCriteria.setFieldname("@ID");
                custCriteria.setOperand("EQ");
                custCriteria.setValue(custNumber);
                filterCriteria.add(custCriteria);
                LOGGER.log(Level.WARNING, "Error processing customer arrangement for customer: " + custNumber, e);
            }
        }

        // Process account number if provided
        if (!acctNumber.isEmpty()) {
            try {
                AccountRecord acctRec = new AccountRecord(da.getRecord("ACCOUNT", acctNumber));
                String aaId = acctRec.getArrangementId().toString();
                
                if (!aaId.isEmpty()) {
                    FilterCriteria acctCriteria = new FilterCriteria();
                    acctCriteria.setFieldname("@ID");
                    acctCriteria.setOperand("EQ");
                    acctCriteria.setValue(aaId);
                    filterCriteria.add(acctCriteria);
                    LOGGER.fine("Added account arrangement filter: " + aaId);
                } else {
                    LOGGER.warning("No arrangement ID found for account: " + acctNumber);
                    FilterCriteria acctCriteria = new FilterCriteria();
                    acctCriteria.setFieldname("@ID");
                    acctCriteria.setOperand("EQ");
                    acctCriteria.setValue(acctNumber);
                    filterCriteria.add(acctCriteria);
                }
            } catch (Exception e) {
                
                FilterCriteria acctCriteria = new FilterCriteria();
                acctCriteria.setFieldname("@ID");
                acctCriteria.setOperand("EQ");
                acctCriteria.setValue(acctNumber);
                filterCriteria.add(acctCriteria);
                LOGGER.log(Level.WARNING, "Error processing account: " + acctNumber, e);
            }
        }

        LOGGER.info("Returning filterCriteria with size: " + filterCriteria.size());
        return filterCriteria;
    }
}