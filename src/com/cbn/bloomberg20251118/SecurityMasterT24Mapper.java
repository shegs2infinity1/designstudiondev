package com.cbn.bloomberg;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.records.securitymaster.SecurityMasterRecord;

public class SecurityMasterT24Mapper {
    private static final Logger logger = Logger.getLogger(SecurityMasterT24Mapper.class.getName());

    public boolean mapToT24Records(SecurityMasterCBN securityMaster,
                                   List<TStructure> records,
                                   List<SynchronousTransactionData> transactionData) {
        try {
            logger.info("Mapping security master to T24: " + securityMaster.mnemonic);

            //TStructure securityRecord = new TStructure();
            SecurityMasterRecord securityRecord = new SecurityMasterRecord();

            //securityRecord.setField("MNEMONIC", securityMaster.mnemonic);
            securityRecord.setMnemonic(securityMaster.mnemonic);
            securityRecord.setShortName(securityMaster.shortName, 0);
            //securityRecord.setField("SHORT.NAME", securityMaster.shortName);
            com.temenos.t24.api.records.securitymaster.DescriptClass  firstdesc = securityRecord.getDescript(0);
            firstdesc.set(securityMaster.description, 0);
            securityRecord.setDescript(firstdesc, 0);

            if (securityMaster.securityDomicile != null) {
                securityRecord.setDomicileRegion(securityMaster.securityDomicile);
            }
            if (securityMaster.bondOrShare != null) {
                securityRecord.setBondOrShare(securityMaster.bondOrShare);
            }

            if (securityMaster.priceCurrency != null) {
                securityRecord.setPriceCurrency(securityMaster.priceCurrency);
            }
            if (securityMaster.priceType != null) {
                securityRecord.setPriceType(securityMaster.priceType);
            }
            if (securityMaster.lastPrice != null) {
                securityRecord.setLastPrice(securityMaster.lastPrice);
            }

            if ("BOND".equals(securityMaster.bondOrShare)) {
                if (securityMaster.interestRate != null) {
                    com.temenos.t24.api.records.securitymaster.InterestRateClass intrate = securityRecord.getInterestRate().get(0);
                    intrate.setInterestRate(securityMaster.interestRate);
                    securityRecord.setInterestRate(intrate, 0);
                }
                if (securityMaster.issueDate != null) {
                    securityRecord.setIssueDate(securityMaster.issueDate);
                }
                if (securityMaster.maturityDate != null) {
                    securityRecord.setMaturityDate(securityMaster.maturityDate);
                }
//                if (securityMaster.numberOfPayment != null) {
//                    securityRecord.setNum
//                    securityRecord.setField("NO.OF.PAYMENTS", securityMaster.numberOfPayment);
//                }
                if (securityMaster.accrualStartDate != null) {
                    securityRecord.setAccrualStartDate(formatT24Date(securityMaster.accrualStartDate));
                }
                if (securityMaster.intPaymentDate != null) {
                    securityRecord.setIntPaymentDate(formatT24Date(securityMaster.intPaymentDate));
                }
                if (securityMaster.firstCpnDate != null) {
                    securityRecord.setFirstCouponDate(formatT24Date(securityMaster.firstCpnDate));
                }
            }

            if (securityMaster.isin != null) {
                securityRecord.setISIN(securityMaster.isin);
            }

            if (securityMaster.setupDate != null) {
                securityRecord.setSetUpDate(securityMaster.setupDate);
            }

            records.add(securityRecord.toStructure());
            logger.info("Successfully mapped security master to T24: " + securityMaster.mnemonic);
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error mapping security master to T24", e);
            return false;
        }
    }

    private String formatT24Date(String bloombergDate) {
        if (bloombergDate == null || bloombergDate.length() != 8) {
            return bloombergDate;
        }
        return bloombergDate; // adjust to your T24 date format if needed
    }
}