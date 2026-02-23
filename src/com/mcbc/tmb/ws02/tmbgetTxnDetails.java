package com.mcbc.tmb.ws02;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebcommonparamtmb.EbCommonParamTmbRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Get the Funds Transfer details from History record EB.API -
 * E.GET.FT.DETAILS.TMB Conversion Routine Method SetValues
 * 
 *
 * @author Debesh
 *
 */
public class tmbgetTxnDetails extends Enquiry {

    String ftId;
    Boolean notfound = false;
    FundsTransferRecord ftrec;
    FundsTransferRecord ftrecHis;
    Boolean debugg = false;
    DataAccess da = new DataAccess(this);
    String delim = "#";
    CompanyRecord compRec = null;
    String CompMnem = null;
    EbCommonParamTmbRecord ecprec;
    List<String> ExcepVersion = new ArrayList<String>();
    List<String> ExcepCurrency = new ArrayList<String>();

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        // TODO Auto-generated method stub
        ftId = value;
        ftId = ftId.trim();
        EbCommonParamTmbRecord ecprec = new EbCommonParamTmbRecord(da.getRecord("EB.COMMON.PARAM.TMB", "TELLER.PARAM"));
        ebcomp ebc = new ebcomp();
        ebc.setEcprec(ecprec);
        ebc.setKeyName("VERSION");   
        ebc.getComp();


        
        
       // ebc.getMapValues();
        List<TField> savingList = ebc.getValuesList();

//        System.out.println("Map Values for Version " + savingList);
        for (TField Acc : savingList) {
            String VExtract = Acc.toString();
            System.out.println("Account Numbers " + VExtract);
           String[] newArr = VExtract.split("#");
           System.out.println("Successfully extracted");
           ExcepVersion.add(newArr[0]);
           ExcepCurrency.add(newArr[1]);
           
        }

String VersionIdSearch = "FCY.OPRET.F07.TMB";
int indexVersion = ExcepVersion.indexOf(VersionIdSearch);
if (indexVersion == -1 )
{
//    System.out.println("Version Does not exists");
    }
else {
//    System.out.println("Version is at Index " + indexVersion);
//    System.out.println("Version is at currency pos " + ExcepCurrency.get(indexVersion));
    
}

        /*
         * Map<String,List<TField>> paramMap = new HashMap
         * <String,List<TField>>();
         * 
         * List<ParamNameClass> ParamName = ecprec.getParamName(); for
         * (ParamNameClass param : ParamName){
         * System.out.println(param.toString());
         * System.out.println("param Names " + param.getParamName().toString()
         * ); System.out.println("param Values " +
         * param.getParamValue().toString());
         * paramMap.put(param.getParamName().toString(),param.getParamValue());
         * 
         * } System.out.println("Map Values" + paramMap); List<TField>
         * savingList = paramMap.get("SAVINGS");
         * System.out.println("Map Values for Savings " + savingList); for
         * (TField Acc : savingList){ System.out.println("Account Numbers " +
         * Acc.toString() ); }
         */

        if (debugg)
            System.out.println("Value of FT ID:" + ftId);
        Session userSession = new Session(this);
        String companyid = userSession.getCompanyId();
        compRec = new CompanyRecord(da.getRecord("COMPANY", companyid));
        CompMnem = compRec.getMnemonic().toString();
        if (debugg)
            System.out.println("Company Mnemonic " + CompMnem);
        try {
            ftrec = new FundsTransferRecord(da.getRecord("FUNDS.TRANSFER", ftId));

        } catch (Exception e) {
            notfound = true;
//            System.out.println("Live record does not exists , so check history");
//            System.out.println(e);

        }
        if (notfound) {
            try {
                ftrec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", ftId));
                notfound = false;
            } catch (Exception e) {
//                System.out.println("Not found in History");
//                System.out.println(e);
                notfound = true;
            }

            if (notfound) {
                try {
                    ftrec = new FundsTransferRecord(da.getRecord(CompMnem, "FUNDS.TRANSFER", "$NAU", ftId));
//                    System.out.println("found in Nau  " + ftrec.toString());
                    notfound = false;
                } catch (Exception e) {
                    System.out.println("Not found in Nau");
                    System.out.println(e);
                    notfound = true;
                }
            }
        }
        value = "";
        if (!notfound) {
            String txnType = ftrec.getTransactionType().toString();
            String DebitAcct = ftrec.getDebitAcctNo().toString();
            // String DebitAmt = ftrec.getDebitAmount().toString();
            String AmtDebited = ftrec.getAmountDebited().toString();
            String CreditAcct = ftrec.getCreditAcctNo().toString();
            // String CreditAmt = ftrec.getCreditAmount().toString();
            String AmtCredited = ftrec.getAmountCredited().toString();
            String CreditCcy = ftrec.getCreditCurrency().toString();
            String DebitCcy = ftrec.getDebitCurrency().toString();
            String ExchangeRate = ftrec.getTreasuryRate().toString();
            int paydetcount = ftrec.getPaymentDetails().size();
            String PayDetails = "";
            if (paydetcount > 0) {
                PayDetails = JoinPaydetails(ftrec.getPaymentDetails());
                // PayDetails = ftrec.getPaymentDetails(0).toString();
            }

            // 1 2 3 4 5 6 7 8 9
            value = txnType + delim + DebitAcct + delim + DebitCcy + delim + AmtDebited + delim + CreditAcct + delim
                    + CreditCcy + delim + AmtCredited + delim + ExchangeRate + delim + PayDetails;

        }
        if (debugg)
            System.out.println(value);
        return value;

    }

    /**
     * @param paymentDetails
     * @return
     */
    private String JoinPaydetails(List<TField> paymentDetails) {
        String Paydetails = "";
        try {
            StringBuilder sb = new StringBuilder();
            int nopay = paymentDetails.size();
            if (nopay > 0) {

                for (int i = 0; i < nopay; i += 1) { // Loop until the index
                                                     // reaches the array length
                    String paydetpart = paymentDetails.get(i).toString();

                    sb.append(paydetpart);
                }
            }

            Paydetails = sb.toString();
            if (debugg) {

                System.out.println("In joining part " + Paydetails);
            }

        } catch (Exception esjoin) {
//            System.out.println("Join Exception" + esjoin);
        }

        // TODO Auto-generated method stub
        return Paydetails;
    }

}
