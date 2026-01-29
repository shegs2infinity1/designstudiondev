package com.mcbc.tmb.ws02;

import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.time.LocalDate;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * This class builds filter criteria for loan deposit arrangements in T24
 * system. It specifically handles AA type accounts by extracting the
 * arrangement ID.
 *
 * @author shegs
 *
 */
public class AaLoanDepositBuild extends Enquiry {

    private static final Logger LOGGER = Logger.getLogger(AaLoanDepositBuild.class.getName());
//    private static final String TAFJ_HOME = System.getenv("TAFJ_HOME");
//    private static final String LOG_DIRECTORY = (TAFJ_HOME != null ? TAFJ_HOME : "/opt") + "/log";
//    private static final String LOG_FILE_PATH = LOG_DIRECTORY + "/AaLoanDepositBuild" + LocalDate.now() + ".log";
    private static final String ACCOUNT_FIELD = "ARRANGEMENT.ID";

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

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        DataAccess da = new DataAccess(this);
        String acctNumber = "";

        String enqID = enquiryContext.getEnquiryId();

        try {
            for (FilterCriteria criteria : filterCriteria) {
                String fieldName = criteria.getFieldname();
                String value = criteria.getValue();
                if (ACCOUNT_FIELD.equalsIgnoreCase(fieldName) && !value.isEmpty()) {
                    acctNumber = value;
                    LOGGER.fine("Account number retrieved: " + acctNumber);
                    String AACheck = acctNumber.substring(0, 2);
                    if ("AA".equals(AACheck)) {
                        AaArrangementRecord aaArr = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", acctNumber));
                        String PrdLine = aaArr.getProductLine().toString();
                        if (enqID.equals("AA.API.NOF.DEPOSIT.ARRANGEMENT.DETAILS.TMB.1.0.0")
                                && PrdLine.equals("DEPOSITS")) {
                            return filterCriteria;
                        }
                        if (enqID.equals("AA.API.NOF.LOAN.ARRANGEMENT.DETAILS.TMB.1.0.0")
                                && PrdLine.equals("LENDING")) {
                            return filterCriteria;
                        }
                        ;
                        filterCriteria.clear();
                        return filterCriteria;
                    }
                    try {
                        AccountRecord Account = new AccountRecord(da.getRecord("ACCOUNT", acctNumber));
                        String AA_ID = Account.getArrangementId().toString();
                        AaArrangementRecord aaArr = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", AA_ID));
                        String PrdLine = aaArr.getProductLine().toString();
                        LOGGER.fine("aa ID extracted from Account Number" + AA_ID);
                        // Clear existing criteria and add filter for authorized
                        // arrangements
                        filterCriteria.clear();
                        FilterCriteria arrStatusCriteria = new FilterCriteria();
                        arrStatusCriteria.setFieldname("ARRANGEMENT.ID");
                        arrStatusCriteria.setOperand("EQ");
                        arrStatusCriteria.setValue(""); //This is to nullify the ID for blank result
                        if (enqID.equals("AA.API.NOF.DEPOSIT.ARRANGEMENT.DETAILS.TMB.1.0.0")
                                && PrdLine.equals("DEPOSITS")) {
                            arrStatusCriteria.setValue(AA_ID);
                            LOGGER.fine("Set AA ID as filter criteria");
                        }
                        if (enqID.equals("AA.API.NOF.LOAN.ARRANGEMENT.DETAILS.TMB.1.0.0")
                                && PrdLine.equals("LENDING")) {
                            {
                                arrStatusCriteria.setValue(AA_ID);
                                LOGGER.fine("Set AA ID as filter criteria");
                            }
                        }
                        filterCriteria.add(arrStatusCriteria);
                    } catch (com.temenos.api.exceptions.T24CoreException e) {
                        LOGGER.fine("Account Number does not exist " + acctNumber);
                    }

                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error extracting filter criteria values", e);
            filterCriteria.clear();
            return filterCriteria;
        }

        return filterCriteria;
    }

}