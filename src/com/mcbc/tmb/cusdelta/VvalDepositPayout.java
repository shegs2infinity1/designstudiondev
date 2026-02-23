package com.mcbc.tmb.cusdelta;

import java.math.BigDecimal;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddessettlement.AaPrdDesSettlementRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.ebcontractbalances.EbContractBalancesRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me! Routine Validates the Payout Amount -
 * 
 * @author Debesh
 * jar file name - CSD_TmbCusDelta.jar
 *
 */
public class VvalDepositPayout extends ActivityLifecycle {
    boolean debugg = false;
    String ftId;
    String termAmountVal;
    String workBalances;
    String payInAccount;
    EbContractBalancesRecord ecbrec;
    AaPrdDesTermAmountRecord aaTermAmtRec;
    AaPrdDesSettlementRecord aaSetlRec = null;
    DataAccess da = new DataAccess(this);

    
    
    @Override
    public TValidationResponse validateRecord(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record) {
     
        String arrId = arrangementActivityRecord.getArrangement().toString();
        if (debugg)
            System.out.println("Inside calling routine VvalDepositPayout");
        try {
            Contract conDets = new Contract(this);
            conDets.setContractId(arrId);
            AaPrdDesTermAmountRecord aaTermAmtRec = new AaPrdDesTermAmountRecord(
                    conDets.getConditionForProperty("COMMITMENT"));
            // termAmountVal = new
            // BigDecimal(aaTermAmtRec.getAmount().getValue());
            termAmountVal = (aaTermAmtRec.getAmount().getValue());
            if (debugg) {
                System.out.println("Term Amount" + termAmountVal);

            }
        } catch (Exception econtract) {
            System.out.println("Error creating Contract :" + econtract);
        }
        try {

            aaSetlRec = new AaPrdDesSettlementRecord(record);
            if (debugg)
                System.out.println("Settle/mentRecord" + aaSetlRec.toString());
            payInAccount = aaSetlRec.getPayinCurrency().get(0).getDdMandateRef(0).getPayinAccount().toString();
            if (debugg)
                System.out.println("payInAccount " + payInAccount);
        } catch (Exception PayinError) {
            System.out.println("Error reading PayIn Account" + PayinError);
        }

        try {
            ecbrec = new EbContractBalancesRecord(da.getRecord("EB.CONTRACT.BALANCES", payInAccount));
            workBalances = ecbrec.getWorkingBalance().toString();
            System.out.println("workBalancesworkBalances" + workBalances);

            if (debugg)
                System.out.println("Reading Work Balances " + workBalances);
        } catch (Exception ecbError) {
            System.out.println("Error Reading ECB " + ecbError);
        }

        try {
            BigDecimal wb;
            BigDecimal tAmount;
            wb = new BigDecimal(workBalances);
            tAmount = new BigDecimal(termAmountVal);
            // workingbalance is small and tAmount is Big
            if (wb.compareTo(tAmount) < 0) {
                if (debugg)
                    System.out.println("catch Insuff error");
                    aaSetlRec.getPayinCurrency().get(0).getDdMandateRef(0).getPayinAccount()
                        .setError("solde insuffisant");
                  
            }

        } catch (Exception AmtConvException) {
            System.out.println("Error Conversion " + AmtConvException);
        }
         return aaSetlRec.getValidationResponse();
    }

}
