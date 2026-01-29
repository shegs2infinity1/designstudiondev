package com.mcbc.tmb.cu;

import java.util.List;
import java.util.logging.Logger;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Enquiry hook to retrieve and format the status of payment orders in T24.
 * Takes a string of payment order IDs separated by ']' and returns their statuses
 * in the same format. If a record is not found, it attempts to fetch the historical
 * record status. Returns an empty string for invalid or non-existent records.
 *
 * @author Segun
 */
public class SetValStoStatus extends Enquiry {

    /**
     * Processes payment order IDs and returns their statuses.
     *
     * @param value           Input string containing payment order IDs separated by ']'.
     * @param currentId       Current record ID (not used in this implementation).
     * @param currentRecord   Current T24 record structure (not used in this implementation).
     * @param filterCriteria  List of filter criteria for the enquiry.
     * @param enquiryContext  Context of the enquiry execution.
     * @return A string containing the statuses of payment orders, separated by ']'.
     * 
     */
    
    private static final Logger LOGGER = Logger.getLogger(SetValStoStatus.class.getName());
    
    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        LOGGER.info("Received Value from Enquiry " + value);
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        DataAccess dataObj = new DataAccess(this);
        StringBuilder result = new StringBuilder();
        String[] parts = value.split("]");

        if (parts.length == 0) {
            return "";
        }

        boolean firstStatus = true;
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }

            String poStatus = "";
            try {
                // Try fetching current record from live table using 2-param getRecord
                TStructure record = null;
                try {
                    record = dataObj.getRecord("PAYMENT.ORDER", part);
                } catch (com.temenos.api.exceptions.T24CoreException e) {
                    // Record does not exist in live table, ignore and try history
                }

                if (record != null) {
                    PaymentOrderRecord poRec = new PaymentOrderRecord(record);
                    poStatus = poRec.getCurrentState() != null ? poRec.getCurrentState().getValue() : "";
                } else {
                    try {
                        TStructure histRecord = dataObj.getHistoryRecord("PAYMENT.ORDER", part);
                        //TStructure histRecord = dataObj.getRecord("", "PAYMENT.ORDER", "$HIS", part + ";2");
                        if (histRecord != null) {
                            PaymentOrderRecord poRec = new PaymentOrderRecord(histRecord);
                            poStatus = poRec.getCurrentState() != null ? poRec.getCurrentState().getValue() : "";
                        }
                    } catch (com.temenos.api.exceptions.T24CoreException e) {
                        // Record does not exist, ignore and try next
                    }

                    // If still empty, try with ;1 suffix
//                    if (poStatus.isEmpty()) {
//                        try {
//                            TStructure histRecord = dataObj.getRecord("", "PAYMENT.ORDER", "$HIS", part + ";1");
//                            if (histRecord != null) {
//                                PaymentOrderRecord poRec = new PaymentOrderRecord(histRecord);
//                                poStatus = poRec.getCurrentState() != null ? poRec.getCurrentState().getValue() : "";
//                            }
//                        } catch (com.temenos.api.exceptions.T24CoreException e) {
//                            // Record does not exist, ignore
//                        }
//                    }
                }
            } catch (Exception e) {
                LOGGER.info("Unable to access record "+ part + " encountered error " + e);
            }

            if (!poStatus.isEmpty()) {
                if (!firstStatus) {
                    result.append("]");
                }
                result.append(poStatus);
                firstStatus = false;
            }
        }

        return result.toString();
    }
}