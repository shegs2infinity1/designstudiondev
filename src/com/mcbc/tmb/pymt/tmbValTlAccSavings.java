package com.mcbc.tmb.pymt;

import java.util.ArrayList;
import java.util.List;


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
 * @author Debesh * /** * @category EB.API - V.VAL.TELLER.SAVINGSAC.TMB
 * 
 * @param Application
 *            - TELLER
 * @param VALIDATE.ROUTINE
 *            - Raise error if the Account is not savings and in CDF currency
 * @param Version
 *            - TELLER,LCY.OPRET.E01.TMB CSD_Payment_Validation.jar
 * @return none
 *
 */

public class tmbValTlAccSavings extends RecordLifecycle {
    DataAccess da = new DataAccess(this);
    TellerRecord tellerRec;
    boolean debugg = false;
    String dbAccount = "";
    String dbcurrency = "";
    String wdAccount;
    String dbCategory = "";
    String lIdOther = "";
    String lAddOther = "";
    String lNumOther = "";
    TField AccField;
    boolean validAccount = false;
    boolean validCurrency = false;
    AccountRecord accRec;
    String VersionName;
    EbCommonParamTmbRecord ecprec;
    String CcyCheck;
    List<String> ExcepVersion = new ArrayList<String>();
    List<String> ExcepCurrency = new ArrayList<String>();
    List<String> savingsAcc = null;
    List<String> dbAccList = null;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        savingsAcc = new ArrayList<String>();
        List<String> dbAccList = new ArrayList<String>();
        GetAccountCategory();
        /*
        savingsAcc.add("6001");
        savingsAcc.add("6002");
        savingsAcc.add("6003"); 
        */
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
            try {
                wdAccount = tellerRec.getAccount2().toString();
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

        if (savingsAcc.contains(dbCategory)) {
            validAccount = true;

            if (debugg) {
                System.out.println("Valid Category  :" + dbCategory);

            }

        } else {
            validAccount = false;
            System.out.println("Not valid Account");
        }

        if ((!validAccount) || (!validCurrency)) {

            for (Account1Class accClass : tellerRec.getAccount1()) {
                AccField = accClass.getAccount1();
                System.out.println("Inside Error Loop");
                if (!validCurrency) {
                    AccField.setError("TL-NOT.VALID.SAVINGS.CCY");
                }
                if (!validAccount) {
                    AccField.setError("TL-NOT.VALID.SAVINGS.ACCOUNT");
                }
            }

        }

        currentRecord.set(tellerRec.toStructure());
        return tellerRec.getValidationResponse();

    }

    /**
     * 
     */

    private void GetAccountCategory() {
        // TODO Auto-generated method stub
        ecprec = new EbCommonParamTmbRecord(da.getRecord("EB.COMMON.PARAM.TMB", "TELLER.PARAM"));
        ebcomp ebc = new ebcomp();
        ebc.setEcprec(ecprec);
        ebc.setKeyName("SAVINGS");
        ebc.getComp();

        // ebc.getMapValues();
        List<TField> CategoryList = ebc.getValuesList();
        try {
            for (TField Categ : CategoryList) {
                String VExtract = Categ.toString();
                if (debugg)
                    System.out.println("categ Extarcts " + VExtract);

                savingsAcc.add(VExtract);
            }
        } catch (Exception Classerr) {
            System.out.println("Error Setting Class Object " + Classerr);
        }
    }

    /**
     * @return
     */
    private String CheckExceptionVersion() {
        // check if the Version is listed in EB.COMMON.PARAM and the return the
        // associated
        // currency type to be checked.
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

}
