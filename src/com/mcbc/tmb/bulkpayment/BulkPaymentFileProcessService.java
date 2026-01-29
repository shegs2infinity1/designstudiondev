package com.mcbc.tmb.bulkpayment;

/**
 * TODO: Document me!
 *
 * @author t.kpohraror
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.temenos.api.TBoolean;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.party.Account;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbRecord;
import com.temenos.t24.api.records.ebbulkpaymenthdrtrltmb.EbBulkpaymentHdrTrlTmbRecord;
import com.temenos.t24.api.records.ebinterfaceloadtmb.EbInterfaceLoadTmbRecord;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebbulkpaymentclearingtmb.EbBulkpaymentClearingTmbTable;
import com.temenos.t24.api.tables.ebbulkpaymenthdrtrltmb.EbBulkpaymentHdrTrlTmbTable;

public class BulkPaymentFileProcessService extends ServiceLifecycle {

    /**
     * This method is used to input the current version of the captured record
     *
     * @category PGM.FILE - CSD.B.BULKPAYMENTFILE.TMB
     * @category EB.API - CSD.B.BULKPAYMENTFILE.TMB
     * @category EB.API - CSD.B.BULKPAYMENTFILE.TMB.SELECT
     * @param Batch - BNK/CSD.B.BULKPAYMENTFILE.TMB
     * @category TSA.SERVICE - BNK/CSD.B.BULKPAYMENTFILE.TMB
     * @param TransactionContext - To process the SICA direct debit internal file -
     *                           BNK
     * @return none
     */

    List<String> recIds = new ArrayList<>();
    String RecName = "BULKPAYMENT.TMB";
    DataAccess da = new DataAccess(this);
    Session session = new Session(this);
    BufferedReader bufferedReader;

    EbBulkpaymentClearingTmbRecord outRec = new EbBulkpaymentClearingTmbRecord(this);
    EbBulkpaymentClearingTmbTable outTab = new EbBulkpaymentClearingTmbTable(this);

    EbBulkpaymentHdrTrlTmbRecord hdrRec = new EbBulkpaymentHdrTrlTmbRecord(this);
    EbBulkpaymentHdrTrlTmbTable hdrTab = new EbBulkpaymentHdrTrlTmbTable(this);
    EbBulkpaymentHdrTrlTmbRecord hdrRecCheck = new EbBulkpaymentHdrTrlTmbRecord(this);
    AccountRecord acctRec = new AccountRecord(this);

    String inpath = "";
    String bkpath = "";
    String dkpath = "";
    String erpath = "";
    Path path;
    Path temp;
    long lines = 0;
    long detline = 0;
    String filecontent;
    String iFileFullPath;
    String msgcontent;
    BufferedReader br;
    String[] msgContentList;
    String hdrrec = "";
    String ftrrec = "";
    String crAcct = "";
    String drAcct = "";
    String txnAmt = "";
    String txnCcy = "";
    String orderingCustomer = "";
    String paymentNarrative = "";

    String header = "";
    String trailer = "";
    String paymentType = "";
    String customerId = "";
    String accountNumber = "";
    String currency = "";
    String bulkCreditDebit = "";
    String washAccount = "";
    String multiSingleEntry = "";
    String narrative = "";
    String numSerieOp = "";
    String licenseNumber = "";
    String receiverBic = "";
    String totalCount = "";
    String totalAmount = "";
    String t24Today = session.getCurrentVariable("!TODAY");
    String processingCompany = "";
    String userInputter = "";
    String bulkFileName = "";

    String bctheaderContent = "";
    String bctrecordContent = "";
    String bcttxnReference = "";
    String bcterrorInfo = "";
    String bctcreditAcct = "";
    String bctdebitAcct = "";
    String bcttxnCcy = "";
    String bctamount = "";
    String bctorderingCustomer = "";
    String bctpaymentNarrative = "";
    String bctbeneficiaryName = "";
    String bctbeneficiaryAddress = "";
    String bctlicenseNumber = "";
    String bctbulkFileName = "";
    String bctfileRecordStatus = "";
    String bctfileRecProcessDate = "";
    String bctfileRecProcessTime = "";

    StringBuffer finalMsgContent = new StringBuffer();
    String currentRecord = "";
    String CurId = "";
    String finalFileName = "";
    String ofsErrorDelimiter = "/-1/NO,";
    Path source;
    Path dest;
    Path source1;
    Path dest1;
    int msglen = 0;
    int actlen = 0;

    boolean debugg = true;

    public void getPaths() {
        try {
            DataAccess intload = new DataAccess(this);

            EbInterfaceLoadTmbRecord recIntload = new EbInterfaceLoadTmbRecord(
                    intload.getRecord("EB.INTERFACE.LOAD.TMB", RecName));

            inpath = recIntload.getDepositDir().getValue();
            if (debugg)
                System.out.println("inpath VALUE " + inpath);

            bkpath = recIntload.getArchiveDir().getValue();
            if (debugg)
                System.out.println("bkpath VALUE " + bkpath);

            dkpath = recIntload.getLoadDir().getValue();
            if (debugg)
                System.out.println("dkpath VALUE " + dkpath);

            erpath = recIntload.getErrorDir().getValue();
            if (debugg)
                System.out.println("erpath VALUE " + erpath);
        }

        catch (T24CoreException arrerror) {
            // if (debugg) {
            System.out.println("No record found for EB.INTERFACE.LOAD.TMB>" + RecName);
            arrerror.printStackTrace();
            // }
        }
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        if (debugg)
            System.out.println("***************getIds**********************************");

        getPaths();

        File directoryPath = new File(inpath);
        if (debugg)
            System.out.println("directoryPath VALUE " + directoryPath);

        String[] contents = directoryPath.list();
        if (debugg)
            System.out.println("contents VALUE " + Arrays.toString(contents));

        List<String> recIds = Arrays.asList(contents);
        if (debugg)
            System.out.println("recIds VALUE " + recIds);

        return recIds;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        // String todaysDate = getTodaysDate();
        getPaths();

        if (debugg)
            System.out.println("***************updateRecord**********************************");

        CurId = "";
        CurId = id;
        boolean newFile = true;
        try {
            hdrRecCheck = new EbBulkpaymentHdrTrlTmbRecord(da.getRecord("EB.BULKPAYMENT.HDR.TRL.TMB", CurId));
            String hdrrecCont = hdrRecCheck.getHeader().getValue();
            if (!hdrrecCont.equals("")) {
                newFile = false;
                System.out.println("File " + CurId + " already processed");
            }
        } catch (Exception ne) {
            if (debugg)
                System.out.println("File " + CurId + " to be processed");
        }
        if (newFile) {
            SynchronousTransactionData txnData = new SynchronousTransactionData();
            inpath = inpath + File.separator + CurId;

            if (debugg)
                System.out.println("CurId VALUE " + CurId);
            String[] rfileName = CurId.split("[.]", -1);
            int fileParts = rfileName.length;
            String ExtnRec = CurId.split("[.]", -1)[fileParts - 1];
            if (debugg)
                System.out.println("ExtnRec VALUE " + ExtnRec);

            String thrDig = CurId.split("[.]", -1)[0];
            if (debugg)
                System.out.println("thrDig VALUE " + thrDig);

            if (ExtnRec.equals("csv")) {

                source = Paths.get(inpath);
                if (debugg)
                    System.out.println("source VALUE " + source);

                String bkuppath = bkpath + File.separator + CurId;
                dest = Paths.get(bkuppath);
                if (debugg)
                    System.out.println("dest VALUE " + dest);

                filecontent = readFileContent(inpath);

                try {
                    Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                if (!filecontent.equals("")) {
                    try {
                        msgContentList = filecontent.split("\n");
                        msglen = msgContentList.length;
                        actlen = msglen - 1;
                        if (debugg)
                            System.out.println("actlen VALUE " + actlen);
                        hdrrec = msgContentList[0];
                        ftrrec = msgContentList[actlen];
                        if (debugg)
                            System.out.println("hdrrec VALUE " + hdrrec);
                        if (debugg)
                            System.out.println("ftrrec VALUE " + ftrrec);
                        try {
                            String[] hdrrecSplit = hdrrec.split(",", -1);
                            int hlen = hdrrecSplit.length;
                            String[] ftrrecSplit = ftrrec.split(",", -1);
                            int flen = ftrrecSplit.length;
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
                            if (hlen > 9) {
                                licenseNumber = hdrrecSplit[9];
                            }
                            if (hlen > 10) {
                                processingCompany = hdrrecSplit[10];
                            }
                            if (hlen > 14) {
                                receiverBic = hdrrecSplit[14];
                            }

                            if (flen > 0) {
                                totalCount = ftrrecSplit[0];
                            }
                            if (flen > 1) {
                                totalAmount = ftrrecSplit[1];
                            }
                        } catch (Exception de) {
                            System.out.println(
                                    "Error extracting values from header/footer details: " + hdrrec + "\n" + ftrrec);
                            System.out.println("Error msg " + de);
                        }
                        hdrRec.setHeader(hdrrec);
                        hdrRec.setTrailer(ftrrec);
                        hdrRec.setPaymentType(paymentType);
                        hdrRec.setCustomerId(customerId);
                        hdrRec.setAccountNumber(accountNumber);
                        hdrRec.setCurrency(currency);
                        hdrRec.setBulkCreditDebit(bulkCreditDebit);
                        hdrRec.setWashAccount(washAccount);
                        hdrRec.setMultiSingleEntry(multiSingleEntry);
                        hdrRec.setNarrative(narrative);
                        hdrRec.setNumSerieOp(numSerieOp);
                        hdrRec.setReceiverBic(receiverBic);
                        hdrRec.setTotalCount(totalCount);
                        hdrRec.setTotalAmount(totalAmount);
                        hdrRec.setFileStatus("UPLOADED");
                        hdrRec.setProcessDate(t24Today);

                        // Check availability of funds
                        boolean oKtoProcess = true;
                        if (multiSingleEntry.equals("SINGLE") && (bulkCreditDebit.equals("CREDIT"))) {
                            if (!accountNumber.equals("")) {
                                AccountRecord accountRecord = new AccountRecord(da.getRecord("ACCOUNT", accountNumber));

                                Account account = new Account(this);
                                account.setAccountId(accountNumber);
                                String availablePlusLimitBalance = account.getAvailableBalance().getAmount().getValue()
                                        .toString();

                                String aAAccountCurrency = accountRecord.getCurrency().getValue();
                                long availableBalance = stringToLong(availablePlusLimitBalance);
                                if (availableBalance < 0) {
                                    oKtoProcess = false;
                                    String eMessage = "Insufficient Available Balance " + aAAccountCurrency
                                            + availablePlusLimitBalance;
                                    hdrRec.setNarrative(eMessage);
                                }
                            }
                        }

                        if (debugg)
                            System.out.println("hdrRec VALUE " + hdrRec);

                        String responseId = CurId;

                        bulkFileName = responseId;
                        System.out.println("bulkFileName " + bulkFileName);

                        String[] parts = bulkFileName.split("\\.");
                        userInputter = parts[0];

                        txnData.setFunction("INPUT");
                        txnData.setSourceId("OFS.LOAD");
                        txnData.setNumberOfAuthoriser("0");

                        try {
                            TStructure userRec = da.getRecord("USER", userInputter);
                            UserRecord user = new UserRecord(userRec);

                            if (user != null) {
                                //txnData.setUserName(userInputter);
                            }
                        } catch (Exception e) {
                            // User not found, continue without setting userName
                        }

                        if (!processingCompany.equals("")) {
                            try {
                                CompanyRecord compRecCheck = new CompanyRecord(
                                        da.getRecord("COMPANY", processingCompany));
                                if (!compRecCheck.equals(null)) {
                                    txnData.setCompanyId(processingCompany);
                                }
                            } catch (Exception comprecEx) {
                                System.out.println("Missing comoany record: " + processingCompany);

                            }
                        }
                        txnData.setVersionId("EB.BULKPAYMENT.HDR.TRL.TMB,INPUT.TMB");
                        txnData.setTransactionId(CurId);
                        txnData.setResponseId(responseId);
                        transactionData.add(txnData);
                        records.add(hdrRec.toStructure());
                        TBoolean exists = null;
                        OfsRequestDetailRecord ofsRequestDetailRecord = da.getRequestResponse(responseId, exists);
                        String msgOut = ofsRequestDetailRecord.getMsgOut().getValue();
                        if (msgOut.equals("") && (!responseId.equals(""))) {

                            try {
                                TStructure ofsRequestDetailTRecord = da.getRecord("OFS.REQUEST.DETAIL", responseId);
                                ofsRequestDetailRecord = new OfsRequestDetailRecord(ofsRequestDetailTRecord);
                                msgOut = ofsRequestDetailRecord.getMsgOut().getValue();
                            } catch (Exception ccORD) {

                                System.out.println("Exception ccORD = " + ccORD.toString());
                            }

                        }
                        boolean ofsError = false;
                        if (debugg)
                            System.out.println("msgOut=" + msgOut);
                        if (debugg)
                            System.out.println("exists=" + exists);

                        if (msgOut.contains(ofsErrorDelimiter)) {
                            ofsError = true;
                        }
                        if (ofsError) {

                            hdrRec.setHeader(msgOut);
                            try {
                                // hdrTab.write(CurId, hdrRec);
                                System.out.println("Writing hdrRec= " + hdrRec.toString());
                                System.out.println("hdrTab.write(CurId, hdrRec)=" + hdrTab.write(CurId, hdrRec));

                            } catch (T24IOException e2) {
                                System.out.println("hdrRec " + hdrRec.toString());
                                e2.printStackTrace();

                            }

                        }

                        if (oKtoProcess) {
                            if (debugg)
                                System.out.println("Processing " + (actlen - 1) + " records");
                            for (int i = 1; i < actlen; i++) {

                                currentRecord = msgContentList[i];
                                String[] splitCurrentRecord = currentRecord.split(",");
                                if (debugg)
                                    System.out.println("Processing currentRecord=" + currentRecord);
                                if (debugg)
                                    System.out.println("splitCurrentRecord.length=" + splitCurrentRecord.length);
                                bctheaderContent = hdrrec;
                                bctrecordContent = currentRecord;

                                if (bulkCreditDebit.equals("DEBIT")) {
                                    if (currentRecord.split(",", -1).length > 0) {
                                        bctdebitAcct = currentRecord.split(",", -1)[0];
                                    }
                                    if (multiSingleEntry.equals("MULTI")) {
                                        bctcreditAcct = accountNumber;
                                    } else {
                                        bctcreditAcct = washAccount;
                                    }
                                } else {
                                    if (currentRecord.split(",", -1).length > 0) {
                                        bctcreditAcct = currentRecord.split(",", -1)[0];
                                    }
                                    if (multiSingleEntry.equals("MULTI")) {
                                        bctdebitAcct = accountNumber;
                                    } else {
                                        bctdebitAcct = washAccount;
                                    }

                                }

                                bcttxnCcy = currency;
                                if (currentRecord.split(",", -1).length > 1) {
                                    bctamount = currentRecord.split(",", -1)[1];
                                }
                                bctorderingCustomer = customerId;
                                if (currentRecord.split(",", -1).length > 2) {
                                    bctpaymentNarrative = currentRecord.split(",", -1)[2];
                                }
                                if (debugg)
                                    System.out.println("bctpaymentNarrative=" + bctpaymentNarrative);
                                if (currentRecord.split(",", -1).length > 3) {
                                    bctbeneficiaryName = currentRecord.split(",", -1)[3];
                                }
                                if (debugg)
                                    System.out.println("bctbeneficiaryName=" + bctbeneficiaryName);
                                if (currentRecord.split(",", -1).length > 4) {
                                    bctbeneficiaryAddress = currentRecord.split(",", -1)[4];
                                }
                                if (currentRecord.split(",", -1).length > 5) {
                                    String bctbeneficiaryAddress2 = currentRecord.split(",", -1)[5];
                                    if (!bctbeneficiaryAddress2.equals("")) {
                                        bctbeneficiaryAddress = bctbeneficiaryAddress + " " + bctbeneficiaryAddress2;
                                    }
                                }
                                if (currentRecord.split(",", -1).length > 6) {
                                    String bctbeneficiaryAddress3 = currentRecord.split(",", -1)[6];
                                    if (!bctbeneficiaryAddress3.equals("")) {
                                        bctbeneficiaryAddress = bctbeneficiaryAddress + " " + bctbeneficiaryAddress3;
                                    }
                                }

                                if (debugg)
                                    System.out.println("bctbeneficiaryAddress=" + bctbeneficiaryAddress);

                                bctlicenseNumber = licenseNumber;
                                if (debugg)
                                    System.out.println("bctlicenseNumber=" + bctlicenseNumber);

                                bctbulkFileName = CurId;
                                bctfileRecordStatus = "UPLOADED";

                                finalFileName = CurId + "-" + i;

                                String dkfile = dkpath + File.separator + finalFileName;
                                if (debugg)
                                    System.out.println("Writing record file " + dkfile);

                                File files = new File(dkfile);

                                try {
                                    FileWriter file = new FileWriter(dkfile);
                                    file.append(hdrrec);
                                    file.append("\n");
                                    file.append(currentRecord);
                                    file.append("\n");
                                    file.append(ftrrec);

                                    file.close();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }

                                outRec.setHeaderContent(bctheaderContent);
                                outRec.setRecordContent(bctrecordContent);

                                outRec.setCreditAcct(bctcreditAcct);
                                outRec.setDebitAcct(bctdebitAcct);
                                outRec.setTxnCcy(bcttxnCcy);
                                outRec.setAmount(bctamount);
                                outRec.setOrderingCustomer(bctorderingCustomer);
                                outRec.setPaymentNarrative(bctpaymentNarrative);
                                outRec.setBeneficiaryName(bctbeneficiaryName);
                                outRec.setBeneficiaryAddress(bctbeneficiaryAddress);
                                outRec.setLicenseNumber(bctlicenseNumber);
                                outRec.setBulkFileName(bctbulkFileName);
                                outRec.setFileRecordStatus(bctfileRecordStatus);
                                outRec.setFileRecProcessDate(t24Today);

                                if (debugg)
                                    System.out.println("crAcct VALUE " + bctcreditAcct);
                                if (debugg)
                                    System.out.println("drAcct VALUE " + bctdebitAcct);
                                if (debugg)
                                    System.out.println("txnCcy VALUE " + bcttxnCcy);
                                if (debugg)
                                    System.out.println("txnAmt VALUE " + bctamount);
                                if (debugg)
                                    System.out.println("orderingCustomer VALUE " + bctorderingCustomer);
                                if (debugg)
                                    System.out.println("paymentNarrative VALUE " + bctpaymentNarrative);
                                try {
                                    System.out.println("finalFileName= " + finalFileName);
                                    System.out.println("Writing outRec= " + outRec.toString());
                                    System.out.println("outTab.write(finalFileName, outRec)="
                                            + outTab.write(finalFileName, outRec));
                                } catch (T24IOException e2) {
                                    System.out.println("finalFileName" + finalFileName);
                                    System.out.println("outRec " + outRec.toString());
                                    e2.printStackTrace();
                                }

                                try {
                                    if (files.createNewFile()) {
                                        if (debugg)
                                            System.out.println("File " + dkfile + " created");
                                    } else {
                                        if (debugg)
                                            System.out.println("File " + dkfile + " already exists");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                            if (multiSingleEntry.equals("SINGLE")) {
                                bctheaderContent = hdrrec;
                                bctrecordContent = hdrrec;

                                if (bulkCreditDebit.equals("DEBIT")) {
                                    bctdebitAcct = washAccount;
                                    bctcreditAcct = accountNumber;

                                } else {
                                    bctdebitAcct = accountNumber;
                                    bctcreditAcct = washAccount;
                                }

                                bcttxnCcy = currency;
                                bctamount = totalAmount;
                                bctorderingCustomer = customerId;
                                bctpaymentNarrative = narrative;
                                bctbulkFileName = CurId;
                                bctfileRecordStatus = "UPLOADED";

                                finalFileName = CurId + "-" + actlen;

                                String dkfile = dkpath + File.separator + finalFileName;
                                File files = new File(dkfile);

                                try {
                                    FileWriter file = new FileWriter(dkfile);
                                    file.append(hdrrec);
                                    file.append("\n");
                                    file.append(ftrrec);

                                    file.close();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }

                                outRec.setHeaderContent(bctheaderContent);
                                outRec.setRecordContent(bctrecordContent);

                                outRec.setCreditAcct(bctcreditAcct);
                                outRec.setDebitAcct(bctdebitAcct);
                                outRec.setTxnCcy(bcttxnCcy);
                                outRec.setAmount(bctamount);
                                outRec.setOrderingCustomer(bctorderingCustomer);
                                outRec.setPaymentNarrative(bctpaymentNarrative);
                                outRec.setBulkFileName(bctbulkFileName);
                                outRec.setFileRecordStatus(bctfileRecordStatus);
                                outRec.setFileRecProcessDate(t24Today);

                                if (debugg)
                                    System.out.println("crAcct VALUE " + bctcreditAcct);
                                if (debugg)
                                    System.out.println("drAcct VALUE " + bctdebitAcct);
                                if (debugg)
                                    System.out.println("txnCcy VALUE " + bcttxnCcy);
                                if (debugg)
                                    System.out.println("txnAmt VALUE " + bctamount);
                                if (debugg)
                                    System.out.println("orderingCustomer VALUE " + bctorderingCustomer);
                                if (debugg)
                                    System.out.println("paymentNarrative VALUE " + bctpaymentNarrative);
                                try {
                                    System.out.println("finalFileName= " + finalFileName);
                                    System.out.println("Writing outRec= " + outRec.toString());
                                    System.out.println("outTab.write(finalFileName, outRec)="
                                            + outTab.write(finalFileName, outRec));
                                } catch (T24IOException e2) {
                                    System.out.println("finalFileName" + finalFileName);
                                    System.out.println("outRec " + outRec.toString());
                                    e2.printStackTrace();
                                }

                                try {
                                    if (files.createNewFile()) {
                                        if (debugg)
                                            System.out.println("File " + dkfile + " created");
                                    } else {
                                        if (debugg)
                                            System.out.println("File " + dkfile + " already exists");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }

            } else {
                String innpath = inpath + File.separator + CurId;
                source1 = Paths.get(innpath);
                String errpath = erpath + File.separator + CurId;
                dest1 = Paths.get(errpath);
                try {
                    Files.move(source1, dest1, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e5) {
                    e5.printStackTrace();
                }
            }
        }
    }

    public String readFileContent(String iFileFullPath) {

        try {
            File f = new File(iFileFullPath);
            br = new BufferedReader(new FileReader(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        try {
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
                if (line != null) {
                    sb.append("\n");
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();

    }

    public String getTodaysDate() {
        String todaysDate = "";
        todaysDate = java.time.LocalDate.now().toString().replace("-", "");

        return todaysDate;
    }

    public Long stringToLong(String myString) {
        long myLong;
        try {
            myLong = Long.parseLong(myString);
        } catch (NumberFormatException ie) {
            // Handle the case where the string is not a valid integer
            myLong = 0;
        }
        return myLong;
    }

}
