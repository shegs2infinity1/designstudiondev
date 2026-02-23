package com.mcbc.tmb.cu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ebcommonparamtmb.EbCommonParamTmbRecord;
import com.temenos.t24.api.records.ebcommonparamtmb.ParamNameClass;
import com.temenos.t24.api.tables.ebcommonparamtmb.EbCommonParamTmbTable;
import com.temenos.t24.api.records.tsaservice.TsaServiceRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author Harshu Veer: added code to write file name to common parameter
 */
public class PreUploadCheck extends ServiceLifecycle {
    DataAccess dataAccess = new DataAccess(this);
    // private static final String BASE_PATH =
    // "/u01/t24appl/r23test/t24/bnk/UD/DATA.IN/AUTO.BILLING.ODBX/";
    // private static final String PRE_LOAD_DIR =
    // "/u01/t24appl/r23test/t24/bnk/UD/DATA.IN/AUTO.BILLING.ODBX/preupload";
    // private static final String LOAD_DIR =
    // "/u01/t24appl/r23test/t24/bnk/UD/DATA.IN/AUTO.BILLING.ODBX/load";
    private static String BASE_PATH = "";
    private static String LOAD_DIR = "";
    private static String PREUP_DIR = "";

    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        // List<String> fileNames = new ArrayList();
        // List<String> FileToProcess = new ArrayList();
        DataAccess dataAccess = new DataAccess(this);
        EbCommonParamTmbRecord paramRecord = new EbCommonParamTmbRecord(
                dataAccess.getRecord("EB.COMMON.PARAM.TMB", "AUTOMATIC.BILLING.PATH"));
        System.out.println("In the select -------   " + paramRecord.toString());
        for (ParamNameClass param : paramRecord.getParamName()) {
            if (param.getParamName().getValue().contains("AUTO.BILL.PATH")) {
                BASE_PATH = param.getParamValue().get(0).getValue();
                LOAD_DIR = BASE_PATH + "load";
                PREUP_DIR = BASE_PATH + "preupload";
                System.out.println("Base Path ------"+ BASE_PATH);
                System.out.println("Load Path ------"+ LOAD_DIR);
                System.out.println("Preup Path -----"+ PREUP_DIR);
                break;
            }
        }
        
        

        List<String> fileNames = new ArrayList<String>();
        List<String> FileToProcess = new ArrayList<String>();
        File PreloadFolder = new File(PREUP_DIR);
        File loadFolder = new File(LOAD_DIR);
        if (PreloadFolder.exists() && PreloadFolder.isDirectory()) {
            String[] filesCount = loadFolder.list();
            if (filesCount != null && filesCount.length > 0) {
                System.out.println("The folder is not empty.");
                return fileNames;
            } else {
                String[] prefilesCount = PreloadFolder.list();
                if (prefilesCount != null && prefilesCount.length > 0) {
                    System.out.println("The folder is not empty.");
                    File[] files = PreloadFolder.listFiles();
                    System.out.println("List all Files---" + files.toString());
                    File file = files[0];
                    String fileName = file.getName();
                    System.out.println("Selected File---" + fileName);
                    FileToProcess.add(fileName);
                } else {
                    System.out.println("The folder is empty.");
                }

                return FileToProcess;
            }
        } else {
            System.out.println("The load directory does not exist: PREUP_DIR");
            return fileNames;
        }
    }

    // Veer start write file name to parameter table
    private void updateCommonParamRecord(EbCommonParamTmbRecord ebCommonParamTmbRecord, String fileName) {
        System.out.println("updateCommonParamRecord");
        for (ParamNameClass ParamName : ebCommonParamTmbRecord.getParamName()) {
            System.out.println("entered the for loop:" + ParamName);
            if (ParamName.getParamName().getValue().contains("FILE.NAME")) {
                System.out.println("get value contains fileName:" + fileName);
                ParamName.getParamValue().get(0).setValue(fileName);
                ParamName.setParamValue(fileName, 0);
                System.out.println("set value contains fileName 0 :" + fileName);
                ebCommonParamTmbRecord.setParamName(ParamName, 0);
                System.out.println("Get value of ParamName :" + ParamName);

            }
        }

    }
    // Veer End

    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        // Veer Start get parameter
        try {
            EbCommonParamTmbTable ebCommonnParamTmbTble = new EbCommonParamTmbTable(this);
            EbCommonParamTmbRecord ebCommonParamTmbRecord = new EbCommonParamTmbRecord(
                    dataAccess.getRecord("EB.COMMON.PARAM.TMB", "AUTOMATIC.BILLING.FILE.NAME"));
            System.out.println("EB.COMMON.PARAM.TMB START");
            updateCommonParamRecord(ebCommonParamTmbRecord, id);
            ebCommonnParamTmbTble.write("AUTOMATIC.BILLING.FILE.NAME", ebCommonParamTmbRecord);

            System.out.println("Parameter update end");

            // SynchronousTransactionData tdThree = new
            // SynchronousTransactionData();
            // tdThree.setTransactionId("AUTOMATIC.BILLING.FILE.NAME");
            // tdThree.setFunction("INPUT");
            // tdThree.setSourceId("BULK.OFS");
            // tdThree.setNumberOfAuthoriser("0");
            // tdThree.setVersionId("EB.COMMON.PARAM.TMB");
            // transactionData.add(tdThree);
            // records.add(ebCommonParamTmbRecord.toStructure());
        } catch (Exception e) {
            // Handle any exceptions that might occur
            System.err.println("An error occurred while writing to the parameter table: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Write end");
        // Veer End

        String filetoMove = PREUP_DIR + id;
        Path sourceFile = Paths.get(filetoMove);
        Path destinationFolder = Paths.get(LOAD_DIR);
        String newFileName = "OBDX-AUTOMATEDBILLING.txt";
        Path destinationFile = destinationFolder.resolve(newFileName);

        try {
            Files.move(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            TsaServiceRecord dmTs = new TsaServiceRecord(
                    this.dataAccess.getRecord("TSA.SERVICE", "DM.SERVICE-OBDX.AUTO.BILLING.TMB"));
            dmTs.getServiceControl().set("START");
            records.add(dmTs.toStructure());
            SynchronousTransactionData verionOfs = new SynchronousTransactionData();
            verionOfs.setSourceId("BUILD.CONTROL");
            verionOfs.setTransactionId("DM.SERVICE-OBDX.AUTO.BILLING.TMB");
            verionOfs.setVersionId("TSA.SERVICE");
            verionOfs.setFunction("INPUT");
            verionOfs.setNumberOfAuthoriser("0");
            transactionData.add(verionOfs);
            System.out.println("Transaciton Data---" + transactionData.toString());
        } catch (IOException var14) {
        }

    }
}