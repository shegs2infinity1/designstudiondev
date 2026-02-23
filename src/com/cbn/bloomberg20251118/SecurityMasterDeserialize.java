package com.cbn.bloomberg;

/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */
import org.json.JSONObject;

public class SecurityMasterDeserialize {

    public SecurityMasterCBN getSecurityMaster(String securityMasterJson) {
        JSONObject json = new JSONObject(securityMasterJson);

        SecurityMasterCBN sm = new SecurityMasterCBN();
        sm.description = json.optString("DESCRIPTION");
        sm.shortName = json.optString("SHORT.NAME");
        sm.mnemonic = json.optString("MNEMONIC");
        sm.securityDomicile = json.optString("SECURITY.DOMICILE");
        sm.bondOrShare = json.optString("BOND.OR.SHARE");
        sm.priceCurrency = json.optString("PRICE.CURRENCY");
        sm.priceType = json.optString("PRICE.TYPE");
        sm.lastPrice = json.optString("LAST.PRICE");
        sm.interestRate = json.optString("INTEREST.RATE");
        sm.issueDate = json.optString("ISSUE.DATE");
        sm.maturityDate = json.optString("MATURITY.DATE");
        sm.numberOfPayment = json.optString("NO.OF.PAYMENT");
        sm.accrualStartDate = json.optString("ACCRUAL.START.DATE");
        sm.intPaymentDate = json.optString("INT.PAYMENT.DATE");
        sm.firstCpnDate = json.optString("FIRST.CPN.DATE");
        sm.isin = json.optString("ISIN");
        sm.setupDate = json.optString("SETUP.DATE");

        return sm;
    }
}
