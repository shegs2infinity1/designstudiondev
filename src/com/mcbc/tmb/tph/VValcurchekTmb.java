package com.mcbc.tmb.tph;
import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.pporderentry.PpOrderEntryRecord;
/**
* TODO: Document me!
*
* @author a.katike
*
*/
public class VValcurchekTmb extends RecordLifecycle {

 
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        PpOrderEntryRecord ppoeRec = new PpOrderEntryRecord(currentRecord);
        Boolean flagsuces=false;
     //   TField pgdac = ppoeRec.getDebitaccountcurrency();
        TField pgcac = ppoeRec.getCreditaccountcurrency();
        String pgdacval = ppoeRec.getDebitaccountcurrency().getValue();
        String pgcacval = ppoeRec.getCreditaccountcurrency().getValue();
        if (pgdacval.equals(pgcacval)) {
            pgcac.setError("PT-CCY.DIFF");
        }
        if  ((!pgdacval.equals("CDF")) && (pgcacval.equals("CDF"))){
        flagsuces = true;
        }else if((pgdacval.equals("CDF")) && (!pgcacval.equals("CDF"))){
        flagsuces = true;
        }
        if (!flagsuces){
        pgcac.setError("Au moins une devise en CDF");
        }
       return ppoeRec.getValidationResponse();

 
    }
}
