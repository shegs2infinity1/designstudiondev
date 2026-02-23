package com.mcbc.tmb.pymt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import com.temenos.api.TField;
import com.temenos.api.TNumber;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.st.currencyapi.ExchangeAmount;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebcommonparamtmb.EbCommonParamTmbRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.records.ebcommonparamtmb.ParamNameClass;
import com.temenos.t24.api.records.teller.Account1Class;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.rates.Currency;

/**
 * TODO: Document me!
 * TO COMPUTE THE TRANSACTION THRESHOLD OVERRIDE FOR FCY CASH TRANSACTIONS (Threshold Buy & Sell FCY)
 * EB.API > V.VAL.TELLER.FCY.THRESHOLD.TMB
 * @author shegs
 *
 */
public class TmbTellerFcyThreshold extends RecordLifecycle {
    
    
    private static final Logger logger = Logger.getLogger(TmbTellerFcyThreshold.class.getName());
    private static final String TAFJ_HOME = System.getenv("TAFJ_HOME");
    private static final String LOG_DIRECTORY = (TAFJ_HOME != null ? TAFJ_HOME : "/opt") + "/log";
    private static final String LOG_FILE_PATH = LOG_DIRECTORY + "/TTDebug" + LocalDate.now() + ".log";
    private static final String CURRENCY_CDF = "CDF";

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
    
    

    private TellerRecord tellerRecord;
    private final DataAccess dataAccess = new DataAccess(this);
    private TField localAmountField;
    private TField localCurrencyField;
    private final Currency currencyApi = new Currency(this);
    BigDecimal usdAmount;
    String currencyMarket = "1";
    private String parameterId;
    private final List<String> overrideMessages = new ArrayList<>();
    private EbCommonParamTmbRecord commonParamRecord;
    
    
//    static {
//        try {
//            String logFilePath = "/u01/t24appl/r23uat2/tafj/log/TTDebug.log";
//            FileHandler fileHandler = new FileHandler(logFilePath, true);
//            fileHandler.setFormatter(new SimpleFormatter());
//            fileHandler.setLevel(Level.ALL);
//            logger.addHandler(fileHandler);
//            logger.setLevel(Level.OFF);
//            logger.setUseParentHandlers(false);
//        } catch (IOException | IllegalStateException e) {
//            logger.setLevel(Level.OFF);
//            Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to initialize logger", e);
//        }
//    }
    
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        
        tellerRecord = new TellerRecord(currentRecord);
        String currency = tellerRecord.getCurrency1().toString();
        List<Account1Class> account1List = tellerRecord.getAccount1();

        BigDecimal txnAmountFcy = parseAmount(account1List.get(0).getAmountFcy1().getValue());
        localAmountField = account1List.get(0).getAmountLocal1();
        localCurrencyField = tellerRecord.getCurrency1();
        
        usdAmount = convertAmountToUsd(txnAmountFcy, currency, currencyMarket);
        
        logger.info("USD Equivalent: " + usdAmount);
        
        parameterId = "EXCHANGE.TELLER.THRESHOLD";
        checkThresholdAmount();
        
        return tellerRecord.getValidationResponse();
    }
    
    private void checkThresholdAmount() {
        try {
            overrideMessages.clear();
            commonParamRecord = new EbCommonParamTmbRecord(
                dataAccess.getRecord("EB.COMMON.PARAM.TMB", parameterId)
            );

            List<ParamNameClass> paramNames = commonParamRecord.getParamName();
            int overrideIndex = tellerRecord.getOverride().size() + 1;

            for (ParamNameClass param : paramNames) {
                String paramName = param.getParamName().toString();
//                String overrideCombined = readOverrideDetail(paramName);

                String override = readOverrideDetail(paramName);
                
                if (!override.isEmpty()) {
                    overrideMessages.add(override);
                    tellerRecord.setOverride(override, overrideIndex++);
                }
            }

            if (!overrideMessages.isEmpty()) {
                localAmountField.setOverride(overrideMessages.get(0));
                if (overrideMessages.size() > 1) {
                    localCurrencyField.setOverride(overrideMessages.get(1));
                }
            }
        } catch (Exception e) {
            logger.warning("Error in checkThresholdAmount: " + e.getMessage());
        }
    }

    
    private BigDecimal parseAmount(String amountStr) {
        try {
            return (amountStr != null && !amountStr.trim().isEmpty())
                    ? new BigDecimal(amountStr.trim())
                    : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.warning("Invalid amount: " + amountStr);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal convertAmountToUsd(BigDecimal amount, String currency, String market) {
        logger.fine("Converting to USD: " + amount + " " + currency + " Market: " + market);
        TNumber buyAmount = new TNumber(amount.toPlainString());
        ExchangeAmount exchange = currencyApi.calculateSellAmount(currency, "USD", buyAmount, market, null, null);
        return new BigDecimal(exchange.getDealAmount().getValue().toString());
    }
    private String readOverrideDetail(String paramName) {
        ebcomp helper = new ebcomp();
        helper.setEcprec(commonParamRecord);
        helper.setKeyName(paramName);
        helper.getComp();

        List<TField> values = helper.getValuesList();
        if (values.size() < 3) return "";

        String fromStr = values.get(0).toString();
        String toStr = values.get(1).toString();
        String overrideDetail = values.get(2).toString();

        try {
            BigDecimal fromAmt = new BigDecimal(fromStr);
            BigDecimal toAmt = toStr.isEmpty() ? null : new BigDecimal(toStr);

            if (toAmt == null && usdAmount.compareTo(fromAmt) > 0) {
                return overrideDetail;
            }
            if (toAmt != null && usdAmount.compareTo(fromAmt) > 0 && usdAmount.compareTo(toAmt) <= 0) {
                return overrideDetail;
            }
        } catch (Exception e) {
            logger.warning("Invalid override threshold for param: " + paramName);
        }

        return "";
    }


}
