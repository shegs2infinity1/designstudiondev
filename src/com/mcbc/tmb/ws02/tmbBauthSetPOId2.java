package com.mcbc.tmb.ws02;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.templatehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbRecord;
import com.temenos.t24.api.tables.ebwso2paramtmb.EbWso2ParamTmbRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.pporderentry.PpOrderEntryRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * EB.API - V.BAUTH.SET.PO.ID.TMB
 * Application: PP.ORDER.ENTRY
 * BEFORE AUTH ROUTINE: Updates the EB.WS02.PAYMENT.DETAILS record with the current PO ID
 * Version: PP.ORDER.ENTRY,CTR.OUT.I103S.TMB
 */
public class tmbBauthSetPOId2 extends RecordLifecycle {
    private static final Logger LOGGER = Logger.getLogger(tmbBauthSetPOId2.class.getName());
    
//    private static final String TAFJ_HOME = System.getenv("TAFJ_HOME");
//    private static final String LOG_DIRECTORY = (TAFJ_HOME != null ? TAFJ_HOME : "/opt") + "/log";
//    private static final String LOG_FILE_PATH = LOG_DIRECTORY + "/tmbBauthSetPOId2" + LocalDate.now() + ".log";

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
     
    private FundsTransferRecord ftRec;
    private PpOrderEntryRecord payRec;
    private EbWs02PaymentDetailsTmbRecord ebwsO2PayRec;
    private EbWso2ParamTmbRecord ebwsO2ParamRec;
    private final DataAccess da = new DataAccess(this);
    private String uniqueCode;
    private String versionId;
    private final String ofsId = "BULK.OFS";

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {
        LOGGER.info("Entering tmbBauthSetPOId2 with record ID: " + currentRecordId);

        try {
            initializePaymentOrder(currentRecord);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading transaction for record ID: " + currentRecordId, e);
            return;
        }

        try {
            updatePaymentDetails(currentRecordId, currentRecords, transactionData);
            LOGGER.info("Successfully updated EB.WS02.PAYMENT.DETAILS with PO ID: " + currentRecordId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating EB.WS02.PAYMENT.DETAILS for unique code: " + uniqueCode, e);
        }
    }

    private void initializePaymentOrder(TStructure currentRecord) {
        payRec = new PpOrderEntryRecord(currentRecord);
        uniqueCode = payRec.getLocalRefField("L.UNIQUECODE").getValue().toString();
        LOGGER.info("Initialized Payment Order with unique code: " + uniqueCode);
    }

    private void updatePaymentDetails(String currentRecordId, List<TStructure> currentRecords,
            List<TransactionData> transactionData) {
        ebwsO2PayRec = new EbWs02PaymentDetailsTmbRecord(
                da.getRecord("EB.WS02.PAYMENT.DETAILS.TMB", uniqueCode));
        LOGGER.info("Fetched EB.WS02.PAYMENT.DETAILS record: " + ebwsO2PayRec);

        ebwsO2PayRec.setTranId(currentRecordId);
        currentRecords.add(ebwsO2PayRec.toStructure());

        TransactionData transactions = new TransactionData();
        transactions.setFunction("INPUT");
        transactions.setTransactionId(uniqueCode);
        transactions.setNumberOfAuthoriser("0");
        transactions.setVersionId("EB.WS02.PAYMENT.DETAILS.TMB,INPUT.TMB");
        transactionData.add(transactions);
        LOGGER.info("Transaction data set for unique code: " + uniqueCode);
    }
}