package com.mcbc.tmb.poe;

import java.util.ArrayList;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebcommonparamtmb.EbCommonParamTmbRecord;
import com.temenos.t24.api.records.ebcommonparamtmb.ParamNameClass;
import com.temenos.t24.api.records.pporderentry.PpOrderEntryRecord;
import com.temenos.t24.api.system.DataAccess;
import java.util.logging.Logger;

/**
 * Defaults the ReceiverInstitutionBic field in a PpOrderEntryRecord based on the transaction currency.
 * Retrieves the BIC code from the EB.COMMON.PARAM.BBG table using the "BICCODES" parameter ID.
 * Matches the transaction currency to a parameter name and sets the corresponding BIC value.
 *
 * @author shegs
 */
public class DefaultBic extends RecordLifecycle {


    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        // Initialize the PpOrderEntryRecord and get transaction currency
        final Logger LOGGER = Logger.getLogger(DefaultBic.class.getName());
        PpOrderEntryRecord poeRec = new PpOrderEntryRecord(currentRecord);
        String transCcy = poeRec.getTransactioncurrency().toString();
        String currentbiccode = poeRec.getReceiverinstitutionbic().toString();
        
        // Exit if transaction currency is empty
        if (transCcy == null || transCcy.trim().isEmpty()) {
            return;
        }
        
        if (currentbiccode != null && !currentbiccode.trim().isEmpty()) {
            return;
        }

        // Access the EB.COMMON,PARAM.TMB record
        DataAccess da = new DataAccess(this);
        String paramId = "BICCODES";
        EbCommonParamTmbRecord paramRec = null;
        java.util.List<ParamNameClass> paramNameClassList = null;
        try {
            paramRec = new EbCommonParamTmbRecord(da.getRecord("EB.COMMON.PARAM.TMB", paramId));
            LOGGER.info("Successfully retrieved EB.COMMON.PARAM.TMB " + paramId);
            paramNameClassList = paramRec.getParamName();
            LOGGER.info("Currency Set in  EB.COMMON.PARAM.TMB " + paramNameClassList.toString());
        } catch (Exception e) {
            LOGGER.severe("Error retrieving records from EB.COMMON.PARAM.TMB: " + e.getMessage());
        }
        
        // Check if parameter record exists and has values
        
        if (paramNameClassList == null || paramNameClassList.isEmpty()) {
            return; // No parameters found, exit silently
        }

        // Iterate through parameters to find matching currency
        for (ParamNameClass pnClass : paramNameClassList) {
            String paramName = pnClass.getParamName().toString();
            java.util.List<TField> pValueList = pnClass.getParamValue();
            
            LOGGER.info("The Present ParamValue is : " + pValueList.toString());
            
            // Match currency (case-insensitive for safety)
            if (paramName != null && paramName.equalsIgnoreCase(transCcy)) {
                if (pValueList != null && !pValueList.isEmpty()) {
                    TField bicCode = pValueList.get(0);
                    LOGGER.info("The Chosen BICCODE is : " + bicCode.toString());
                    poeRec.setReceiverinstitutionbic("");
                    poeRec.setReceiverinstitutionbic(bicCode);
                    currentRecord.set(poeRec.toStructure());
                    LOGGER.info("The Present Record is : " + poeRec.toString());
                    break; // Exit loop after setting the BIC
                }
            }
        }
    }

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // Initialize the PPOrderEntryRecord and get transaction currency
        final Logger LOGGER = Logger.getLogger(DefaultBic.class.getName());
        PpOrderEntryRecord poeRec = new PpOrderEntryRecord(currentRecord);
        String transCcy = poeRec.getTransactioncurrency().toString();
        String currentbiccode = poeRec.getReceiverinstitutionbic().toString();
        
        if (currentbiccode != null && !currentbiccode.trim().isEmpty()) {
            return poeRec.getValidationResponse();
        }

        // Access the EB.COMMON,PARAM.TMB record
        DataAccess da = new DataAccess(this);
        String paramId = "BICCODES";
        EbCommonParamTmbRecord paramRec = null;
        java.util.List<ParamNameClass> paramNameClassList = null;
        try {
            paramRec = new EbCommonParamTmbRecord(da.getRecord("EB.COMMON.PARAM.TMB", paramId));
            System.out.println("Successfully Retrieved EB.COMMON.PARAM.TMB " + paramId);
            paramNameClassList = paramRec.getParamName();
            LOGGER.info("Currency Set in  EB.COMMON.PARAM.TMB " + paramNameClassList.toString());
        } catch (Exception e) {
            System.out.println("Error retrieving records from EB.COMMON.PARAM.TMB: " + e.getMessage());
        }
        
        // Check if parameter record exists and has values
        
        if (paramNameClassList == null || paramNameClassList.isEmpty()) {
            return poeRec.getValidationResponse(); // No parameters found, exit silently
        }

        // Iterate through parameters to find matching currency
        for (ParamNameClass pnClass : paramNameClassList) {
            String paramName = pnClass.getParamName().toString();
            java.util.List<TField> pValueList = pnClass.getParamValue();
            
            System.out.println("The Present ParamValue is : " + pValueList.toString());
            
            // Match currency (case-insensitive for safety)
            if (paramName != null && paramName.equalsIgnoreCase(transCcy)) {
                if (pValueList != null && !pValueList.isEmpty()) {
                    TField bicCode = pValueList.get(0);
                    System.out.println("The Chosen BICCODE is : " + bicCode.toString());
                    poeRec.setReceiverinstitutionbic("");
                    poeRec.setReceiverinstitutionbic(bicCode);
                    System.out.println("The Present Record is : " + poeRec.toString());
                    break; // Exit loop after setting the BIC
                }
            }
        }
        
        return poeRec.getValidationResponse();
       
    }
    
}