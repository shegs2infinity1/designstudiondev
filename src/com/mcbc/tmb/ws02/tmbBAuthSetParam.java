package com.mcbc.tmb.ws02;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aclockedevents.AcLockedEventsRecord;
import com.temenos.t24.api.records.ebws02paymentdetailstmb.EbWs02PaymentDetailsTmbRecord;
import com.temenos.t24.api.tables.ebwso2paramtmb.EbWso2ParamTmbRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.pporderentry.PpOrderEntryRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * EB.API - V.BAUTH.SET.PARAM.TMB jar file - CSD_TmbWSO2Dev.jar
 * Application: EB.WSO2.PAYMENT.DETAILS.TMB
 * BEFORE AUTH ROUTINE: Reads details from EB.WS02.PAYMENT.DETAILS and populates
 * FT, AC.LOCKED.EVENTS or PAYMENT.ORDER variables
 * Version: WS02.PAYMENT.DETAILS.TMB
 */
public class tmbBAuthSetParam extends RecordLifecycle {
    private static final Logger LOGGER = Logger.getLogger(tmbBAuthSetParam.class.getName());
//    private static final String TAFJ_HOME = System.getenv("TAFJ_HOME");
//    private static final String LOG_DIRECTORY = (TAFJ_HOME != null ? TAFJ_HOME : "/opt") + "/log";
//    private static final String LOG_FILE_PATH = LOG_DIRECTORY + "/tmbBAuthSetParam" + LocalDate.now() + ".log";
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
      
    private String ftId;
    private String wsO2PaymentRec;
    private FundsTransferRecord ftRec;
    private AcLockedEventsRecord acLockRec;
    private PpOrderEntryRecord payRec;
    private EbWs02PaymentDetailsTmbRecord ebwsO2PayRec;
    private EbWso2ParamTmbRecord ebwsO2ParamRec;
    private final DataAccess da = new DataAccess(this);
    private String ebwsO2InterfaceType;
    private String ebwsO2FTTC;
    private String ebwsO2Chargetype;
    private String ebwsO2TransferType;
    private String ebwsO2ParamId;
    private String ebwsO2MsgId;
    private String ebwsO2DebitAcc;
    private String ebwsO2DebitCcy;
    private String ebwsO2CreditAcc;
    private String ebwsO2CreditCcy;
    private String ebwsO2CDebitAmt = "";
    private String ebwsO2Rate;
    private String ebwsO2CCreditAmt = "";
    private String ebwsO2PaymentDetails;
    private String versionId;
    private final String ofsId = "BULK.OFS";
    private String appName = "";
    private String noOfAuth;
    private List<String> payDetSegment;
    private final String reversalType = "INTREVERSE";
    private final String authType = "INTTRANAUTH";
    private final String authFlag = "UNAUTH";
    private boolean reversalReq = false;
    private boolean forcedAuth = false;
    private String processFunction = "INPUT";
    private String reverseFTId = "";
    private String creditRef = "";
    private String benefAccount = "";
    private String benefName = "";
    private String benefAddr1 = "";
    private String benefAddr2 = "";
    private String benefAddr3 = "";
    private String benefCountry = "";
    private String instIdCode = "";
    private String receiverBic = "";
    private String acctWithInstCode = "";
    private String nRate = "";
    private String debitValueDate = "";
    private String creditValueDate = "";
    private String chargeOptions = "";
    private String license = "";
    private String serieOp = "";
    private List<TField> payDetails;

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<com.temenos.t24.api.complex.eb.templatehook.TransactionData> transactionData,
            List<TStructure> currentRecords) {
        LOGGER.info("Entering tmbBAuthSetParam with record ID: " + currentRecordId);
        
        try {
            initializePaymentDetails(currentRecord, currentRecordId);
            processTransactionType();
            handlePaymentDetails();
            versionId = ebwsO2PayRec.getVersionName().toString();
            ebwsO2InterfaceType = ebwsO2PayRec.getInterfaceType().toString();
            appName = getAppDetails(ebwsO2ParamId);
            LOGGER.info("Application Name: " + appName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing EB.WSO2.PAYMENT.DETAILS: " + e.getMessage(), e);
        }

        try {
            processApplicationRecords(currentRecords);
            setupTransactionData(transactionData);
            LOGGER.info("Transaction data updated: " + transactionData);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating FT/AC Lock/Payment Order: " + e.getMessage(), e);
        }
    }

    private void initializePaymentDetails(TStructure currentRecord, String currentRecordId) {
        ebwsO2PayRec = new EbWs02PaymentDetailsTmbRecord(currentRecord);
        ebwsO2MsgId = ebwsO2PayRec.getMsgId().toString();
        ebwsO2InterfaceType = ebwsO2PayRec.getInterfaceType().toString();
        ebwsO2TransferType = ebwsO2PayRec.getTransferType().toString();
        ebwsO2ParamId = ebwsO2InterfaceType + "-" + ebwsO2TransferType;
        ebwsO2DebitAcc = ebwsO2PayRec.getDebitAcct().toString();
        ebwsO2DebitCcy = ebwsO2PayRec.getDebitCcy().toString();
        ebwsO2CreditAcc = ebwsO2PayRec.getCreditAcct().toString();
        ebwsO2CreditCcy = ebwsO2PayRec.getCreditCcy().toString();
        ebwsO2CDebitAmt = ebwsO2PayRec.getDebitAmt().toString();
        ebwsO2CCreditAmt = ebwsO2PayRec.getCreditAmt().toString();
        ebwsO2Rate = ebwsO2PayRec.getExchangeRate().toString();
        ebwsO2FTTC = ebwsO2PayRec.getTransferType().toString();
        payDetails = ebwsO2PayRec.getPaymentDetails();
        license = ebwsO2PayRec.getLocalRefField("L.LICENSE").toString();
        serieOp = ebwsO2PayRec.getLocalRefField("L.SERIE.OP").toString();
        benefAccount = ebwsO2PayRec.getBenefAccount().toString();
        benefName = ebwsO2PayRec.getBenefName().toString();
        benefAddr1 = ebwsO2PayRec.getBenefAddress1().toString();
        benefAddr2 = ebwsO2PayRec.getBenefAddress2().toString();
        benefAddr3 = ebwsO2PayRec.getBenefAddress3().toString();
        benefCountry = ebwsO2PayRec.getBenefCountry().toString();
        receiverBic = ebwsO2PayRec.getRecieverBic().toString();
        instIdCode = ebwsO2PayRec.getIntInstIdCode().toString();
        debitValueDate = ebwsO2PayRec.getDebitValueDate().toString();
        creditValueDate = ebwsO2PayRec.getCreditValueDate().toString();
        chargeOptions = ebwsO2PayRec.getChargeOptions().toString();
        acctWithInstCode = ebwsO2PayRec.getAcctWithInstIdCode().toString();
        
        
        
        creditRef = currentRecordId;
        LOGGER.info("Initialized EB.WSO2.PAYMENT.DETAILS record: " + ebwsO2PayRec);
    }

    private void processTransactionType() {
        forcedAuth = ebwsO2TransferType.equals(authType);
        LOGGER.info("Forced Auth: " + forcedAuth);
        
        if (ebwsO2CreditCcy.equals(ebwsO2DebitCcy) || forcedAuth) {
            noOfAuth = "0";
        } else {
            noOfAuth = "0";
        }
        //To check for unauth flag 
        if (ebwsO2TransferType != null && ebwsO2TransferType.endsWith(authFlag)) {
            noOfAuth = "1";
        }

        reversalReq = ebwsO2TransferType.equals(reversalType);
        if (reversalReq) {
            noOfAuth = "0";
            processFunction = "REVERSE";
            reverseFTId = ebwsO2PayRec.getTranId().toString();
        }
    }

    private void handlePaymentDetails() {
        if (!payDetails.isEmpty()) {
            getPaymentDetails();
        }
    }

    private void processApplicationRecords(List<TStructure> currentRecords) {
        if (appName.equals("FUNDS.TRANSFER")) {
            processFundsTransfer(currentRecords);
        } else if (appName.equals("PAYMENT.ORDER")) {
            processPaymentOrder(currentRecords);
        } else {
            setupAcLockedEvents();
        }
    }

    private void processFundsTransfer(List<TStructure> currentRecords) {
        if (!reversalReq) {
            setFtRec();
            currentRecords.add(ftRec.toStructure());
            LOGGER.info("Funds Transfer record set: " + ftRec);
        } else {
            try {
                LOGGER.info("Processing reversal for Funds Transfer");
                ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", reverseFTId));
                currentRecords.add(ftRec.toStructure());
                LOGGER.info("History Record Structure: " + ftRec);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "History record not found for ID: " + reverseFTId, e);
            }
        }
    }

    private void processPaymentOrder(List<TStructure> currentRecords) {
        if (!reversalReq) {
            
            LOGGER.info("Now trying to process Payment Order");
            setPayRec();
            currentRecords.add(payRec.toStructure());
            LOGGER.info("Payment Order record set: " + payRec);
        } else {
            try {
                LOGGER.info("Processing reversal for Payment Order");
                payRec = new PpOrderEntryRecord(da.getHistoryRecord("PAYMENT.ORDER", reverseFTId));
                currentRecords.add(payRec.toStructure());
                LOGGER.info("History Record Structure for Payment Order: " + payRec);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "History record not found for ID: " + reverseFTId, e);
            }
        }
    }

    private void setupAcLockedEvents() {
        LOGGER.info("Setting up data for AC Lock Version: " + versionId);
        acLockRec = new AcLockedEventsRecord();
        acLockRec.setTransRef(creditRef);
        acLockRec.setAccountNumber(ebwsO2DebitAcc);
        acLockRec.setDescription(ebwsO2PaymentDetails);
        acLockRec.setLockedAmount(ebwsO2CDebitAmt);
        acLockRec.setTransRef(creditRef);
        LOGGER.info("Ac Locked Events configured");
    }

    private void setupTransactionData(List<com.temenos.t24.api.complex.eb.templatehook.TransactionData> transactionData) {
        LOGGER.info("Setting up transaction data");
        com.temenos.t24.api.complex.eb.templatehook.TransactionData transactions = new com.temenos.t24.api.complex.eb.templatehook.TransactionData();
        transactions.setFunction(processFunction);
        
        if (reversalReq) {
            transactions.setTransactionId(reverseFTId);
            noOfAuth = "0";
        }
        
        transactions.setNumberOfAuthoriser(noOfAuth);
        transactions.setVersionId(versionId);
        transactionData.add(transactions);
    }

    private void getPaymentDetails() {
        String fullPaymentDets = joinPayDetails(payDetails);
        int lenPay = fullPaymentDets.length();
        LOGGER.info("Length of payment details after join: " + lenPay);
        
        final int maxLength = 140;
        fullPaymentDets = fullPaymentDets.substring(0, Math.min(lenPay, maxLength)).trim();
        LOGGER.info("Payment details after joining: " + fullPaymentDets);
        
        //ebwsO2CCreditAmt = ebwsO2PayRec.getCreditAmt().toString();
        int noPay = ebwsO2PayRec.getPaymentDetails().size();
        payDetSegment = stringSplitter(fullPaymentDets, 35);
        LOGGER.info("Payment details after splitting: " + payDetSegment);
        
        if (noPay > 0) {
            ebwsO2PaymentDetails = ebwsO2PayRec.getPaymentDetails(0).toString();
        }
    }

    private void setFtRec() {
        ftRec = new FundsTransferRecord();
        ftRec.setDebitAcctNo(ebwsO2DebitAcc);
        ftRec.setCreditAcctNo(ebwsO2CreditAcc);
        ftRec.setDebitCurrency(ebwsO2DebitCcy);
        ftRec.setCreditCurrency(ebwsO2CreditCcy);
        ftRec.setCreditAmount(ebwsO2CCreditAmt);
        LOGGER.info("Credit Amount :" + ebwsO2CCreditAmt + " and Debit Amount : " + ebwsO2CDebitAmt );
        if (ebwsO2InterfaceType.equals("LOGIREF")) {
            if (ebwsO2CCreditAmt == "") {
                ftRec.setDebitAmount(ebwsO2CDebitAmt);
            }
        }
        ftRec.setCreditValueDate(creditValueDate);
        ftRec.setDebitValueDate(debitValueDate);
        ftRec.setCreditTheirRef(creditRef);
        ftRec.setTreasuryRate(ebwsO2Rate);
        
        setPaymentDetails(ftRec);
        LOGGER.info("Funds Transfer record: " + ftRec);
    }

    private void setPayRec() {
        
        LOGGER.info("Now in the setPayRec Method");
        payRec = new PpOrderEntryRecord(this);
        if (ebwsO2CCreditAmt != null) {
            payRec.setTransactionamount(ebwsO2CCreditAmt);
            payRec.setTransactioncurrency(ebwsO2CreditCcy);
        } else if (ebwsO2CDebitAmt != null) {
            payRec.setTransactionamount(ebwsO2CDebitAmt);
            payRec.setTransactioncurrency(ebwsO2DebitCcy);
        }
        payRec.setDebitaccountnumber(ebwsO2DebitAcc);
        payRec.setBeneficiaryaccount(benefAccount);
        
        payRec.setBeneficiaryname(benefName);
        payRec.setBeneficiaryaddress1(benefAddr1);
        payRec.setBeneficiaryaddress2(benefAddr2);
        payRec.setBeneficiaryaddress3(benefAddr3);
        payRec.setAccountwithinstidentifiercode(acctWithInstCode);
        payRec.setIntermediaryinstidentifiercode(instIdCode);
        payRec.setReceiverinstitutionbic(receiverBic);
        payRec.getLocalRefField("L.UNIQUECODE").setValue(creditRef);
        payRec.getLocalRefField("L.LICENSE").setValue(license);
        payRec.getLocalRefField("L.SERIE.OP").setValue(serieOp);
        payRec.setDebitvaluedate(debitValueDate);
        payRec.setCreditvaluedate(creditValueDate);
        payRec.setChargeoption(chargeOptions);
        setPaymentDetails(payRec);
        LOGGER.info("Payment Order record: " + payRec);
    }

    private void setPaymentDetails(Object record) {
        String[] arrayPayDetConv = payDetSegment.toArray(new String[0]);
        int index = 0;
        for (String part : arrayPayDetConv) {
            LOGGER.info("Payment detail segment [" + index + "]: " + part);
            if (record instanceof FundsTransferRecord) {
                ((FundsTransferRecord) record).setPaymentDetails(part, index);
            } else if (record instanceof PpOrderEntryRecord) {
                ((PpOrderEntryRecord) record).setPaymentdetails(part, index);
            }
            index++;
        }
    }

    private String joinPayDetails(List<TField> paymentDetails) {
        StringBuilder sb = new StringBuilder();
        try {
            for (TField payDetPart : paymentDetails) {
                sb.append(payDetPart.toString());
            }
            String payDetailsStr = sb.toString();
            LOGGER.info("Joined payment details: " + payDetailsStr);
            return payDetailsStr;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error joining payment details: " + e.getMessage(), e);
            return "";
        }
    }

    private List<String> stringSplitter(String input, int chunkSize) {
        List<String> result = new ArrayList<>();
        try {
            int length = input.length();
            for (int i = 0; i < length; i += chunkSize) {
                result.add(input.substring(i, Math.min(length, i + chunkSize)));
            }
            LOGGER.info("Split payment details: " + result);
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error splitting string: " + e.getMessage(), e);
            return result;
        }
    }

    private String getAppDetails(String paramId) {
        String appName = "";
        try {
            EbWso2ParamTmbRecord ebwsO2ParamRec = new EbWso2ParamTmbRecord(
                    da.getRecord("EB.WSO2.PARAM.TMB", ebwsO2ParamId));
            appName = ebwsO2ParamRec.getApplicationName().toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving EB.WSO2.PARAM.TMB for ID: " + ebwsO2ParamId, e);
            ebwsO2PayRec.getTransferType().setError("Invalid Transfer Type / Interface Type");
        }
        return appName;
    }
}