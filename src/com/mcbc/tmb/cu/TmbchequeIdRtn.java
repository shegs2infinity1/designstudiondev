package com.mcbc.tmb.cu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author alyash
 *
 */

/**
 * TODO: Document me!
 *
 * @amender t.kpohraror
 *
 */
public class TmbchequeIdRtn extends RecordLifecycle {

    DataAccess dataAccess = new DataAccess(this);
    List<String> idList = null;
    String chequeId = "";
    String chqType = "";
    String accountId = "";
    String serialNo = "";

    // TODO Auto-generated method stub
    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
//        System.out.println("**** Running method com.mcbc.tmb.cu.TmbchequeIdRtn.checkId ****");

//        System.out.println("currentRecordId: " + currentRecordId);
        chequeId = currentRecordId;
        String[] chequeIdSplit = chequeId.split("[.]", -1);
        int hlen = chequeIdSplit.length;
        if (hlen > 0) {
            chqType = chequeIdSplit[0];
        }
        if (hlen > 1) {
            accountId = chequeIdSplit[1];
        }
        if (hlen > 2) {
            serialNo = chequeIdSplit[2];
        }
//        System.out.println("chqType: " + chqType);
//        System.out.println("accountId: " + accountId);
//        System.out.println("serialNo: " + serialNo);

        try {
            if (!accountId.equals("")) {
                idList = dataAccess.getConcatValues("CHEQUE.ISSUE.ACCOUNT", accountId);
            }
//            System.out.println("idList: " + idList.toString());
            if (idList.isEmpty()) {
//                System.out.println("No Cheques Issued yet for account: " + accountId);
            } else {
                String recId = getMaxChequeIssueId(idList, currentRecordId);
                if (!recId.equals(chequeId)) {

                    int firstDotIndex = recId.indexOf(".");
                    chqType = recId.substring(0, firstDotIndex);

                    int secondDotIndex = recId.indexOf(".", firstDotIndex + 1);
                    accountId = recId.substring(firstDotIndex + 1, secondDotIndex);

                    int thirdDotIndex = recId.lastIndexOf(".");

                    String lastPart = recId.substring(thirdDotIndex + 1);

                    Integer aa = Integer.valueOf(lastPart) + 1;
                    serialNo = String.format("%1$7s", aa).replace(' ', '0');

                    // Create the new string by joining the parts back together
                    chequeId = chqType + "." + accountId + "." + serialNo;
                }
//                System.out.println("chequeId: " + chequeId);

            }

        } catch (Exception e) {
//            System.out.println("Error checking Cheque Issue ID: " + e.getMessage());
            // Handle the exception as per your requirements

        }
        return chequeId;
    }

    public String getMaxChequeIssueId(List<String> idList, String currentRecordId) {
        String maxSerialNo = "0";
        String maxRecId = currentRecordId;

        for (String id : idList) {
            if (id.startsWith(chqType)) {
                int lastDotIndex = id.lastIndexOf(".");
                if (lastDotIndex != -1) {
                    String serialNumberStr = id.substring(lastDotIndex + 1);
                    try {
                        int serialNumber = Integer.parseInt(serialNumberStr);
                        if (serialNumber > Integer.parseInt(maxSerialNo)) {
                            maxRecId = id;
                            maxSerialNo = serialNumberStr;
                        }
                    } catch (Exception e) {
                        // Ignore any invalid serial numbers
                    }
                }
            }
        }

//        System.out.println("maxRecId: " + maxRecId);
        return maxRecId;

    }

    public static String getMaxSerialNumber(List<String> idList, String currentRecordId) {
        if (idList.isEmpty()) {
            return null; // Return null if the list is empty
        }

        List<Integer> serialNumbers = new ArrayList<>();
        for (String id : idList) {
            if (id.startsWith(currentRecordId)) {
                int lastDotIndex = id.lastIndexOf(".");
                if (lastDotIndex != -1) {
                    String serialNumberStr = id.substring(lastDotIndex + 1);
                    try {
                        int serialNumber = Integer.parseInt(serialNumberStr);
                        serialNumbers.add(serialNumber);
                    } catch (NumberFormatException e) {
                        // Ignore any invalid serial numbers
                    }
                }
            }
        }

        if (serialNumbers.isEmpty()) {
            return null; // Return null if no valid serial numbers are found
        }

        Collections.sort(serialNumbers, Collections.reverseOrder());
        int maxSerialNumber = serialNumbers.get(0);
        return currentRecordId + "." + String.format("%07d", maxSerialNumber);
    }

}
