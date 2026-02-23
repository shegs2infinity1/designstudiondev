package com.mcbc.bbg.sn.forex;

import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.forex.ForexRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.mmmoneymarket.MmMoneyMarketRecord;
import com.temenos.t24.api.records.portransaction.PorTransactionRecord;
import com.temenos.t24.api.records.posmvmttoday.PosMvmtTodayRecord;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;

public class EConvPositionMvmtBbgCi extends Enquiry {
    private static String CustName = null;
    private final DataAccess da = new DataAccess();

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        
        String txnref = value;
        System.out.println("The Transction Ref is "+txnref);
        
        try {
            PosMvmtTodayRecord mvtrec = new PosMvmtTodayRecord(currentRecord);
            String sysID = mvtrec.getSystemId().toString();
            System.out.println("System ID "+ sysID );

            switch (sysID) {
                case "FT":
                    CustName = processFundsTransfer(txnref);
                    break;
                case "FX":
                    CustName = processForex(txnref);
                    break;
                case "MM":
                    CustName = processMoneyMarket(txnref);
                    break;
                case "PP":
                    CustName = processPaymentOrder(txnref);
                    break;
                case "TT":
                    CustName = processTeller(txnref);
                    break;
                default:
                    CustName = "Unknown System ID";
            }
        } catch (Exception e) {
            CustName = "Error processing transaction: " + e.getMessage();
            e.printStackTrace();
        }
        
        System.out.println("Customer name is "+CustName);
        
        return CustName;
    }

    private String processFundsTransfer(String txnref) {
        try {
            String orderingCust = null;
            try {
                FundsTransferRecord ftRec = new FundsTransferRecord(da.getRecord("CIV", "FUNDS.TRANSFER", "", txnref));
                System.out.println("FT Record "+ftRec.getOrderingCust(0).toString());
                orderingCust = ftRec.getOrderingCust(0).toString();
            } catch (Exception e){
                System.out.println("Unable to read FT live table");
            }

            if (orderingCust == null || orderingCust.isEmpty()) {
                try{
                    FundsTransferRecord ftHisRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", txnref));
                    System.out.println("FT Record "+ftHisRec.getOrderingCust(0).toString());
                    orderingCust = ftHisRec.getOrderingCust(0).toString();
                } catch (Exception e){
                    System.out.println("Unable to read FT History table");
                }
            }
            
            if (orderingCust == null || orderingCust.isEmpty()) {
                try{
                    FundsTransferRecord nauFTRec = new FundsTransferRecord(da.getRecord("CIV", "FUNDS.TRANSFER", "$NAU", txnref));
                    System.out.println("FT Record "+nauFTRec.getOrderingCust(0).toString());
                    orderingCust = nauFTRec.getOrderingCust(0).toString();
                } catch (Exception e){
                    System.out.println("Unable to read FT unauth table");
                }
            }
            
            return getCustomerName(orderingCust);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving Funds Transfer record: " + e.getMessage();
        }
    }

    private String processForex(String txnref) {
        System.out.println("Processing Forex "+txnref.substring(0, 12));
        try {
            String Counterpty = null;
            try {
                ForexRecord fxRec = new ForexRecord(da.getRecord("CIV", "FOREX","", txnref.substring(0, 12)));
                System.out.println("Reading Live Table for FX");
                Counterpty = fxRec.getCounterparty().toString();
            } catch (Exception e) {
               System.out.println("Unable to Read FX live");
            }
            if (Counterpty == null || Counterpty.isEmpty()){
                System.out.println("Reading History Table for FX");
                try {
                ForexRecord fxRecHis = new ForexRecord(da.getHistoryRecord("FOREX", txnref.substring(0, 12)));
                Counterpty = fxRecHis.getCounterparty().toString();
                }
                catch (Exception e){ 
                }
            }
            if (Counterpty == null || Counterpty.isEmpty()){
                System.out.println("Reading unauth Table for FX");
                try {
                ForexRecord fxRecNau = new ForexRecord(da.getRecord("CIV", "FOREX", "$NAU", txnref.substring(0, 12)));
                Counterpty = fxRecNau.getCounterparty().toString();
                }
                catch (Exception e){
                    
                }
            }
            System.out.println("Finishing for FX");
            return getCustomerName(Counterpty);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving Forex record: " + e.getMessage();
        }
    }

    private String processMoneyMarket(String txnref) {
        try {
            MmMoneyMarketRecord mmRec = new MmMoneyMarketRecord(da.getRecord("MM.MONEY.MARKET", txnref));
            return getCustomerName(mmRec.getCustomerId().toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving Money Market record: " + e.getMessage();
        }
    }

    private String processPaymentOrder(String txnref) {
        System.out.println("Processing Payment Order");
        try {
            PorTransactionRecord PpRec = new PorTransactionRecord(da.getRecord("POR.TRANSACTION", txnref));
            return PpRec.getDebitpartyline1().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving Por Transaction record: " + e.getMessage();
        }
    }
    
    private String processTeller(String txnref) {
        System.out.println("Processing Teller");
        try {
            String Custname = null;
            try {
                TellerRecord TTRec = new TellerRecord(da.getRecord("TELLER", txnref));
                Custname = TTRec.getLocalRefField("L.TT.FX.F.NAME").getValue().toString();
            }   catch (Exception e) {
            
            } 
            if (Custname == null || Custname.isEmpty()){
                try {
                    TellerRecord TTRecHis = new TellerRecord(da.getHistoryRecord("TELLER", txnref));
                    Custname = TTRecHis.getLocalRefField("L.TT.FX.F.NAME").getValue().toString();
                }   catch (Exception e) {
                
                } 
            }
            
            if (Custname == null || Custname.isEmpty()){
                try {
                    TellerRecord TTRecNau = new TellerRecord(da.getRecord("CIV", "TELLER", "$NAU", txnref));
                    Custname = TTRecNau.getLocalRefField("L.TT.FX.F.NAME").getValue().toString();
                }   catch (Exception e) {
                
                } 
            }
            return Custname;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving TT Transaction record: " + e.getMessage();
        }
    }
    
    private String getCustomerName(String custNo) {
        try {
            CustomerRecord custRec = new CustomerRecord(da.getRecord("CUSTOMER", custNo));
            return custRec.getShortName(0).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return custNo;
        }
    }
}
