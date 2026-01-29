package com.mcbc.tmb.pymt;

import java.util.ArrayList;
import java.util.List;

//import com.mcbc.commonUtils.GetCommonParamValue;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.ebcommonparamtmb.EbCommonParamTmbRecord;
import com.temenos.t24.api.records.teller.Account1Class;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author Debesh * /** * @category EB.API - V.VAL.TELLER.CURRENTAC.TMB
 * 
 * @param Application
 *            - TELLER
 * @param VALIDATE.ROUTINE
 *            - Raise error if the Account is not CURRENT and in CDF currency
 * @param Version
 *            - TELLER,LCY.OPRET.E05.TMB CSD_Payment_Validation
 * @return none
 *
 */

public class tmbValTlAccCurrent extends RecordLifecycle {
    DataAccess da = new DataAccess(this);
    TellerRecord tellerRec;
    boolean debugg = false;
    String dbAccount = "";
    String wdAccount = "";
    String dbcurrency = "";
    String dbCategory = "";
    String lIdOther = "";
    String lAddOther = "";
    String lNumOther = "";
    TField AccField = null;
    String VersionName;
    boolean validAccount = false;
    boolean validCurrency = false;
    AccountRecord accRec;
    EbCommonParamTmbRecord ecprec;
    String CcyCheck;
    List<String> ExcepVersion = new ArrayList<String>();
    List<String> ExcepCurrency = new ArrayList<String>();
    List<String> currentAcc = null;
    List<String> dbAccList = null;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        currentAcc = new ArrayList<String>();
        dbAccList = new ArrayList<String>();
        GetAccountCategory();
        /*
        currentAcc.add("1001");
        currentAcc.add("1002");
        currentAcc.add("1003");
        currentAcc.add("1004");
        currentAcc.add("1005");
        currentAcc.add("1006");
        currentAcc.add("1007");
        currentAcc.add("1009");
        currentAcc.add("1010");
        currentAcc.add("1011");
        currentAcc.add("1012");
        
        */
        /*
         * currentAcc.add("1013"); currentAcc.add("1014");
         * currentAcc.add("1015"); currentAcc.add("1016");
         * currentAcc.add("1017");
         * 
         */

        // Get ext connect flag , if set to N , then dont proceed to Verify
        // wallet

        VersionName = transactionContext.getCurrentVersionId();
        if (debugg) System.out.println("Version Name " + VersionName);

        String ExcepCurrency = CheckExceptionVersion();
        if (ExcepCurrency.length() > 0) {
            CcyCheck = ExcepCurrency;
        } else {
            if (VersionName.indexOf("LCY.") > 0) {
                CcyCheck = "LCY";
            } else {
                CcyCheck = "FCY";
            }

        }
        try {
            tellerRec = new TellerRecord(currentRecord);
            if (debugg)
                System.out.println("tellerRec full " + tellerRec);
            AccField = null;

            try {
                wdAccount = tellerRec.getAccount2().toString();
//                System.out.println("withdrawal Account " + wdAccount);
                for (Account1Class accClass : tellerRec.getAccount1()) {
                    dbAccount = accClass.getAccount1().getValue();

                    AccField = accClass.getAccount1();
                    dbAccList.add(dbAccount);
                    if (debugg)
                        System.out.println("dbAccount  :" + dbAccount);
                }
                if (debugg)
                    System.out.println("dbAccList " + dbAccList.toString());
                dbAccount = wdAccount;
            } catch (Exception e) {
                System.out.println(" Missing Account Record:" + e);
            }

            if (dbAccount.length() > 0) {
                accRec = new AccountRecord(da.getRecord("ACCOUNT", dbAccount));
                dbcurrency = accRec.getCurrency().toString();
                dbCategory = accRec.getCategory().toString();

            }

        } catch (Exception e) {
            System.out.println("Account Data Access Exception " + e);
        }

        if (debugg)
            System.out.println("Debit Currency :" + dbcurrency);
        if (debugg)
            System.out.println("Debit dbCategory :" + dbCategory);

        if ((dbcurrency.equals("CDF")) && (CcyCheck.equals("LCY"))) {
            if (debugg)
                System.out.println("Valid Currency :" + dbcurrency);
            validCurrency = true;

        } else {

            if (!(dbcurrency.equals("CDF")) && (CcyCheck.equals("FCY"))) {

                validCurrency = true;
            } else {
                validCurrency = false;
            }
        }

        if (debugg)
            System.out.println("Allowed Category  :" + currentAcc);
        
        if ((currentAcc.contains(dbCategory))) {

            if (debugg)
                System.out.println("Valid Category  :" + dbCategory);
            validAccount = true;

        } else {
            validAccount = false;

        }

        if (!validCurrency) {

            AccField.setError("TL-NOT.VALID.CURRENT.CCY");
        }

        if (!validAccount) {

            AccField.setError("TL-NOT.VALID.CURRENT.ACCOUNT");
        }
        currentRecord.set(tellerRec.toStructure());
        return tellerRec.getValidationResponse();
    }

    String CheckExceptionVersion() {
        String retCurrency = "";
        try {

            ecprec = new EbCommonParamTmbRecord(da.getRecord("EB.COMMON.PARAM.TMB", "TELLER.PARAM"));
            ebcomp ebc = new ebcomp();
            ebc.setEcprec(ecprec);
            ebc.setKeyName("VERSION");
            ebc.getComp();

            // ebc.getMapValues();
            List<TField> VersionList = ebc.getValuesList();

            for (TField Acc : VersionList) {
                String VExtract = Acc.toString();
                if (debugg)
                    System.out.println("Version Extarcts " + VExtract);
                String[] newArr = VExtract.split("#");
                ExcepVersion.add(newArr[0]);
                ExcepCurrency.add(newArr[1]);

            }
            if (debugg) System.out.println("Version Name " + VersionName);
            int indexVersion = ExcepVersion.indexOf(VersionName);
            if (indexVersion == -1) {
                if (debugg)
                    System.out.println("Version Does not exists");
                retCurrency = "";
            } else {
                if (debugg)
                    System.out.println("Version is at Index " + indexVersion);
                if (debugg)
                    System.out.println("Version is at currency pos " + ExcepCurrency.get(indexVersion));
                retCurrency = ExcepCurrency.get(indexVersion);
            }

        } catch (Exception Classerr) {
            System.out.println("Error Setting Class Object " + Classerr);
        }
        return retCurrency;
    }


    private void GetAccountCategory() {
        // TODO Auto-generated method stub
        ecprec = new EbCommonParamTmbRecord(da.getRecord("EB.COMMON.PARAM.TMB", "TELLER.PARAM"));
        ebcomp ebc = new ebcomp();
        ebc.setEcprec(ecprec);
        ebc.setKeyName("CURRENT");
        ebc.getComp();

        // ebc.getMapValues();
        List<TField> CategoryList = ebc.getValuesList();
        try {
            for (TField Categ : CategoryList) {
                String VExtract = Categ.toString();
                if (debugg)
                    System.out.println("categ Extarcts " + VExtract);

                currentAcc.add(VExtract);
            }
        } catch (Exception Classerr) {
            System.out.println("Error Setting Class Object " + Classerr);
        }
    }

}
