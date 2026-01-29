package com.mcbc.tmb.bulkpayment;

/**
 * TODO: Document me!
 *
 * @author t.kpohraror
 *
 */

import java.util.List;

import com.temenos.api.TStructure;
// import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbRecord;
import com.temenos.t24.api.records.pporderentry.PpOrderEntryRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbTable;

public class BulkPaymentCreatePoe extends ServiceLifecycle {
    /**
     * This method is used to input the current version of the captured record
     *
     * @category PGM.FILE - CSD.B.BULKPAYMENT.POE.TMB
     * @category EB.API - CSD.B.BULKPAYMENT.POE.TMB
     * @category EB.API - CSD.B.BULKPAYMENT.POE.TMB.SELECT
     * @param Batch - BNK/CSD.B.BULKPAYMENT.POE.TMB
     * @category TSA.SERVICE - BNK/CSD.B.BULKPAYMENT.POE.TMB
     * @return none
     */

    List<String> retids;
    EbBulkpaymentClearingTmbRecord outRec = new EbBulkpaymentClearingTmbRecord();
    EbBulkpaymentClearingTmbTable outTab = new EbBulkpaymentClearingTmbTable(this);
    PpOrderEntryRecord payRec = new PpOrderEntryRecord(this);
    DataAccess da = new DataAccess(this);
    Session session = new Session(this);
    AccountRecord accRec = new AccountRecord(this);
    CompanyRecord companyRec = new CompanyRecord(this);
    String CurId;
    String hdrrec = "";
    String msgContent1;
    String msgContent2;
    String msgContent3;
    String drAcct;
    String crAcct;
    String txnAmt;
    String userInputter;

    String headerContent = "";
    String recordContent = "";
    String txnReference = "";
    String errorInfo = "";
    String creditAcct = "";
    String debitAcct = "";
    String txnCcy = "";
    String amount = "";
    String orderingCustomer = "";
    String paymentNarrative = "";
    String beneficiaryName = "";
    String beneficiaryAddress = "";
    String licenseNumber = "";
    String bulkFileName = "";
    String fileRecordStatus = "";
    String fileRecProcessDate = "";
    String fileRecProcessTime = "";

    String paymentType = "";
    String customerId = "";
    String accountNumber = "";
    String currency = "";
    String bulkCreditDebit = "";
    String washAccount = "";
    String multiSingleEntry = "";
    String narrative = "";
    String numSerieOp = "";
    String totalCount = "";
    String totalAmount = "";

    String beneficiaryAddress1 = "";
    String beneficiaryAddress2 = "";
    String beneficiaryAddress3 = "";
    String receiverBic = "";
    String accountWithBank = "";
    String intermidiaryBic = "";
    String chargeOption = "";
    String TtcInstructionCode = "";
    String LocInstructionCode = "";

    String localCcy = "";
    String companyId = "";
    String versionId = "PP.ORDER.ENTRY,INPUT.TMB";
    String ofsSourceId = "OFS.LOAD";
    String ofsFunction = "I";
    String ofsNoAuthor = "0";
    String washAccountVersionId = "PP.ORDER.ENTRY,CTR.UPLOAD.AC.COMPTA.TMB";

    String sessionCompanyId = session.getCompanyId();
    String t24Today = session.getCurrentVariable("!TODAY");

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        DatesRecord datesRecord = new DatesRecord(da.getRecord("DATES", sessionCompanyId));

        String t24TransactSystemDate = datesRecord.getToday().getValue();
        String selectionCriteria = "WITH FILE.RECORD.STATUS EQ EXECUTE" + " AND FILE.REC.PROCESS.DATE EQ "
                + t24TransactSystemDate;
        retids = da.selectRecords("", "EB.BULKPAYMENT.CLEARING.TMB", "", selectionCriteria);
        System.out.println("retids " + retids);
        return retids;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        CurId = id;
        System.out.println("CurId " + CurId);
        try {
            TStructure clearingRec = da.getRecord("EB.BULKPAYMENT.CLEARING.TMB", CurId);
            outRec = new EbBulkpaymentClearingTmbRecord(clearingRec);

            msgContent1 = outRec.getRecordContent().toString();
            System.out.println("msgContent1 " + msgContent1);

            String hdrrec = outRec.getHeaderContent().toString();
            System.out.println("hdrrec " + hdrrec);

            bulkFileName = outRec.getBulkFileName().toString();
            System.out.println("bulkFileName " + bulkFileName);

            String[] parts = bulkFileName.split("\\.");
            userInputter = parts[0];
            System.out.println("userInputter " + userInputter);

            fileRecordStatus = outRec.getFileRecordStatus().toString();
            System.out.println("fileRecordStatus " + fileRecordStatus);

            fileRecProcessDate = outRec.getFileRecProcessDate().toString();
            System.out.println("fileRecProcessDate " + fileRecProcessDate);

            fileRecProcessTime = outRec.getFileRecProcessTime().toString();
            System.out.println("fileRecProcessTime " + fileRecProcessTime);

            String[] hdrrecSplit = hdrrec.split(",", -1);
            int hlen = hdrrecSplit.length;
            if (hlen == 10) {
                System.out.println("Header record length was  " + hlen);
                hdrrec = hdrrec + ",";
                hdrrecSplit = hdrrec.split(",", -1);
                hlen = hdrrecSplit.length;
            }

            if (hlen > 0) {
                paymentType = hdrrecSplit[0];
            }
            if (hlen > 1) {
                customerId = hdrrecSplit[1];
            }
            if (hlen > 2) {
                accountNumber = hdrrecSplit[2];
            }
            if (hlen > 3) {
                currency = hdrrecSplit[3];
            }
            if (hlen > 4) {
                bulkCreditDebit = hdrrecSplit[4];
            }
            if (hlen > 5) {
                washAccount = hdrrecSplit[5];
            }
            if (hlen > 6) {
                multiSingleEntry = hdrrecSplit[6];
            }
            if (hlen > 7) {
                narrative = hdrrecSplit[7];
            }
            if (hlen > 8) {
                numSerieOp = hdrrecSplit[8];
            }
            if (hlen > 11) {
                chargeOption = hdrrecSplit[11];
            }
            if (hlen > 12) {
                TtcInstructionCode = hdrrecSplit[12];
            }
            if (hlen > 13) {
                LocInstructionCode = hdrrecSplit[13];
            }

        } catch (Exception be) {
            System.out.println("Error extracting values from header details: " + hdrrec);
            System.out.println("Error msg " + be);
        }
        boolean validChargeOption = (chargeOption.equals("BEN") || chargeOption.equals("OUR")
                || chargeOption.equals("SHA"));
        if (!validChargeOption) {
            chargeOption = "SHA";
        }
        // System.out.println("hdrrec " + hdrrec);
        System.out.println("chargeOption " + chargeOption);

        crAcct = outRec.getCreditAcct().getValue();
        drAcct = outRec.getDebitAcct().getValue();

        String txnAccount = "";
        String accCcy = "";
        if (paymentType.equals("ACTRF")) {
            if (bulkCreditDebit.equals("DEBIT")) {
                txnAccount = drAcct;
            } else {
                txnAccount = crAcct;
            }

            try {
                accRec = new AccountRecord(da.getRecord("ACCOUNT", txnAccount));
            } catch (Exception Invacc) {
                System.out.println("Invalid Account " + txnAccount);
                outRec.setReserved0("Invalid Account " + txnAccount);
            }
            if (!accRec.equals(null)) {
                accCcy = accRec.getCurrency().getValue();
                companyId = accRec.getCoCode();
                if (!companyId.equals("")) {
                    try {
                        companyRec = new CompanyRecord(da.getRecord("COMPANY", companyId));
                        localCcy = companyRec.getLocalCurrency().getValue();
                    } catch (Exception Invcom) {
                        System.out.println("Invalid companyId " + companyId);
                        companyId = session.getCompanyId();
                        localCcy = session.getLocalCurrency();
                    }
                } else {
                    System.out.println("Null Account CompanyId " + companyId);
                    companyId = session.getCompanyId();
                    localCcy = session.getLocalCurrency();
                }
            } else {
                companyId = session.getCompanyId();
                localCcy = session.getLocalCurrency();
            }
        } else {
            companyId = session.getCompanyId();
            localCcy = session.getLocalCurrency();

        }

        txnAmt = outRec.getAmount().getValue();
        txnCcy = outRec.getTxnCcy().getValue();
        orderingCustomer = outRec.getOrderingCustomer().getValue();
        paymentNarrative = outRec.getPaymentNarrative().getValue();
        licenseNumber = outRec.getLicenseNumber().getValue();
        if (!msgContent1.trim().equals(hdrrec.trim())) {
            beneficiaryName = outRec.getBeneficiaryName().getValue();
            // beneficiaryAddress = outRec.getBeneficiaryAddress().getValue();
            // String[] splitAddress = splitAddress(beneficiaryAddress);

            if (msgContent1.split(",", -1).length > 4) {
                beneficiaryAddress1 = msgContent1.split(",", -1)[4];
            }
            if (msgContent1.split(",", -1).length > 5) {
                beneficiaryAddress2 = msgContent1.split(",", -1)[5];
            }
            if (msgContent1.split(",", -1).length > 6) {
                beneficiaryAddress3 = msgContent1.split(",", -1)[6];
            }
            if (msgContent1.split(",", -1).length > 7) {
                receiverBic = msgContent1.split(",", -1)[7];
            }
            if (msgContent1.split(",", -1).length > 8) {
                accountWithBank = msgContent1.split(",", -1)[8];
            }
            if (msgContent1.split(",", -1).length > 9) {
                intermidiaryBic = msgContent1.split(",", -1)[9];
            }
        } else {

            if (hdrrec.split(",", -1).length > 14) {
                receiverBic = hdrrec.split(",", -1)[14];
            }
        }

        // int var1 = Integer.parseInt(txnAmt);
        // String fAmt = String.valueOf(var1);

        /*
         * System.out.println("crAcct " + crAcct); System.out.println("drAcct " +
         * drAcct); System.out.println("txnAmt " + txnAmt); System.out.println("fAmt " +
         * fAmt); System.out.println("txnCcy " + txnCcy);
         * System.out.println("orderingCustomer " + orderingCustomer);
         * System.out.println("paymentNarrative " + paymentNarrative);
         * System.out.println("licenseNumber " + licenseNumber);
         * System.out.println("numSerieOp " + numSerieOp);
         * System.out.println("chargeOption " + chargeOption);
         */
        outRec.setFileRecordStatus("PROCESSING");
        // String todaysDate = getTodaysDate();
        outRec.setFileRecProcessDate(t24Today);
        try {
            // System.out.println("Writing record " + CurId + ":" + "\n" + outRec);
            outTab.write(CurId, outRec);
        } catch (Exception ne) {
            System.out.println("Error writing record " + CurId + " " + ne);
            ne.printStackTrace();
        }
        payRec.setDebitaccountnumber(drAcct.trim());
        if (paymentType.equals("ACTRF")) {
            payRec.setCreditaccountnumber(crAcct.trim());

        } else {
            payRec.setBeneficiaryaccount(crAcct.trim());
            payRec.setOrderingcustomerid(orderingCustomer);
            if (bulkCreditDebit.equals("CREDIT")) {
                // payRec.setReceiverinstitutionbic(receiverBic);
                payRec.setBeneficiaryname(beneficiaryName);
                payRec.setChargeoption(chargeOption);
                if (!beneficiaryAddress1.equals("")) {
                    payRec.setBeneficiaryaddress1(beneficiaryAddress1.trim());
                }
                if (!beneficiaryAddress2.equals("")) {
                    payRec.setBeneficiaryaddress2(beneficiaryAddress2.trim());
                }
                if (!beneficiaryAddress3.equals("")) {
                    payRec.setBeneficiaryaddress3(beneficiaryAddress3.trim());
                }
                if (!receiverBic.equals("")) {
                    payRec.setReceiverinstitutionbic(receiverBic.trim());
                }
                if (!accountWithBank.equals("")) {
                    payRec.setAccountwithinstidentifiercode(accountWithBank.trim());
                }
                if (!intermidiaryBic.equals("")) {
                    payRec.setIntermediaryinstidentifiercode(intermidiaryBic);
                }
                if (!TtcInstructionCode.equals("") && !LocInstructionCode.equals("")) {
                    payRec.setInstructioncode(TtcInstructionCode, 0);
                    payRec.setInstructioncode(LocInstructionCode, 1);

                } else {

                    if (!TtcInstructionCode.equals("")) {
                        payRec.setInstructioncode(TtcInstructionCode, 0);

                    }
                    if (!LocInstructionCode.equals("")) {
                        payRec.setInstructioncode(LocInstructionCode, 0);

                    }
                }
            }

        }

        payRec.setTransactioncurrency(txnCcy.trim());
        payRec.setTransactionamount(txnAmt);
        payRec.setPaymentdetails(paymentNarrative, 0);
        payRec.getLocalRefField("L.FILE.NAME").setValue(CurId);
        payRec.getLocalRefField("L.SERIE.OP").setValue(numSerieOp);
        payRec.getLocalRefField("L.LICENSE").setValue(licenseNumber);
        // System.out.println("payRec " + payRec);

        SynchronousTransactionData txnData = new SynchronousTransactionData();
        String responseId = CurId;
        if (paymentType.equals("ACTRF")) {
            if (!accCcy.equals("")) {
                if (accCcy.equals(txnCcy)) {
                    versionId = "PP.ORDER.ENTRY,CTR.UPLOAD.AC.MEME.TMB";
                }
                if (!accCcy.equals(txnCcy) && (!accCcy.equals(localCcy) && !txnCcy.equals(localCcy))) {
                    versionId = "PP.ORDER.ENTRY,CTR.UPLOAD.AC.DIFF.TMB";
                }
                if (!accCcy.equals(txnCcy) && (accCcy.equals(localCcy) || txnCcy.equals(localCcy))) {
                    versionId = "PP.ORDER.ENTRY,CTR.UPLOAD.AC.CNLOC.TMB";
                }
            } else {
                payRec.setDirection("B");
                payRec.setTransfertype("C");
                payRec.setIncomingmessagetype("RFCT");
            }
        } else {
            if (paymentType.equals("RTGST")) {
                versionId = "PP.ORDER.ENTRY,CTR.UPLOAD.OUT.R103S.TMB";
            }
            if (paymentType.equals("NATRF")) {
                versionId = "PP.ORDER.ENTRY,CTR.UPLOAD.OUT.103.TMB";
            }
            if (paymentType.equals("INTRF")) {
                versionId = "PP.ORDER.ENTRY,CTR.UPLOAD.OUT.I103S.TMB";
            }
            if (paymentType.equals("ACHTR")) {
                versionId = "PP.ORDER.ENTRY,CTR.ISO.O.TMB";
            }

        }
        if ((drAcct.equals(washAccount) && crAcct.equals(accountNumber))
                || (crAcct.equals(washAccount) && drAcct.equals(accountNumber))) {
            versionId = washAccountVersionId;
        }

        txnData.setSourceId(ofsSourceId);
        txnData.setVersionId(versionId);
        txnData.setFunction(ofsFunction);
        txnData.setNumberOfAuthoriser(ofsNoAuthor);
        txnData.setUserName(userInputter);
        txnData.setCompanyId(companyId);
        txnData.setResponseId(responseId);
        System.out.println("txnData " + txnData);

        transactionData.add(txnData);
        records.add(payRec.toStructure());
    }

    public String[] splitAddress(String address) {

        String[] addressArray = { "", "", "" };
        String[] tmpAddress = address.split(" ");
        for (String element : tmpAddress) {
            if ((addressArray[0] + " " + element).length() < 34) {
                addressArray[0] += " " + element;
            }
            if ((addressArray[0] + " " + element).length() >= 34 && (addressArray[1] + " " + element).length() < 34) {
                addressArray[1] += " " + element;
            }
            if ((addressArray[1] + " " + element).length() >= 34 && (addressArray[2] + " " + element).length() < 342) {
                addressArray[2] += " " + element;
            }

        }
        return addressArray;
    }

    public String getTodaysDate() {
        String todaysDate = "";
        todaysDate = java.time.LocalDate.now().toString().replace("-", "");

        return todaysDate;
    }
}
