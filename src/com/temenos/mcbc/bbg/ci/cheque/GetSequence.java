package com.temenos.mcbc.bbg.ci.cheque;

import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.alternateaccount.AlternateAccountRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Retrieves the next available cheque sequence number.
 *
 * @author shegs
 */
public class GetSequence extends Enquiry {

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
                           List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        int nextSeqNo;
        int initSeqno = 1;
        int liveSequence = 0;
        String nextSequence;
        String searchId = value;
        
        DataAccess da = new DataAccess(this);
        System.out.println("Current Value is " + value);
//        AccountRecord accountRec = new AccountRecord();
        AlternateAccountRecord altRec = new AlternateAccountRecord();
        String[] idparts = searchId.split("\\.");
        try {
            System.out.println("Account Number Check" + idparts[1].length());
            if (idparts[1].length() == 12){
//                accountRec = new AccountRecord(da.getRecord("ACCOUNT", searchId));
            } else if (idparts[1].length() == 11) {
                System.out.println("Alter Account Number Check "+ idparts[1].length());
                altRec = new AlternateAccountRecord(da.getRecord("ALTERNATE.ACCOUNT", idparts[1]));
                System.out.println("Alter Account Number "+ altRec);
                String acctnum = altRec.getGlobusAcctNumber().getValue();
                idparts[1] = acctnum ;
                searchId = idparts[0]+'.'+acctnum;
//                accountRec = new AccountRecord(da.getRecord("ACCOUNT", searchId));
            } else {
                System.out.println("Invalid Account Number");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        // Fetch existing cheque issuance records with IDs matching the search pattern
        List<String> chequeIssueLiveId = da.selectRecords("CIV", "CHEQUE.ISSUE", "",
                "WITH @ID LIKE '" + searchId + "...' BY-DSND @ID");

        System.out.println("Live Cheque IDs: " + chequeIssueLiveId);

        if (!chequeIssueLiveId.isEmpty()) {
            try {
                String[] idParts = chequeIssueLiveId.get(0).split("\\.");
                
                liveSequence = Integer.parseInt(idParts[idParts.length - 1]); // Extract last part as sequence
                nextSeqNo = liveSequence + 1;
                System.out.println("Next Sequence is "+nextSeqNo);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Error parsing sequence number: " + e.getMessage());
                nextSeqNo = initSeqno;
            }
        } else {
            nextSeqNo = initSeqno;
        }

        nextSequence = String.format("%07d", nextSeqNo); // Ensure fixed length (e.g., 0000001)
        return nextSequence;
    }
}
