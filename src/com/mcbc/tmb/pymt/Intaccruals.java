package com.mcbc.tmb.pymt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.temenos.api.TField;
import com.temenos.t24.api.records.aainterestaccruals.AaInterestAccrualsRecord;
import com.temenos.t24.api.records.aainterestaccruals.AccrualAmtClass;
import com.temenos.t24.api.records.aainterestaccruals.FromDateClass;


/**
 * TODO: Document me!
 *
 * @author debdas
 *
 */
public class Intaccruals {
    Map<String,List<TField>> paramMap = new HashMap <String,List<TField>>();
    /**
     * @return the paramNames
     */
    public String getParamNames() {
        return ParamNames;
    }
    /**
     * @param paramNames the paramNames to set
     */
    public void setParamNames(String paramNames) {
        ParamNames = paramNames;
    }
    /**
     * @return the fromDate
     */
    public List<TField> getFromDate() {
        return FromDate;
    }
    /**
     * @param fromDate the fromDate to set
     */
    public void setFromDate(List<TField> fromDate) {
        FromDate = fromDate;
    }
    /**
     * @return the toDates
     */
    public List<TField> getToDates() {
        return ToDates;
    }
    /**
     * @param toDates the toDates to set
     */
    public void setToDates(List<TField> toDates) {
        ToDates = toDates;
    }
    /**
     * @return the noDays
     */
    public List<TField> getNoDays() {
        return NoDays;
    }
    /**
     * @param noDays the noDays to set
     */
    public void setNoDays(List<TField> noDays) {
        NoDays = noDays;
    }
    /**
     * @return the keyName
     */
    public String getKeyName() {
        return KeyName;
    }
    /**
     * @param keyName the keyName to set
     */
    public void setKeyName(String keyName) {
        KeyName = keyName;
    }
    
    
    
    /**
     * @return the accrualAmt
     */
    public List<TField> getAccrualAmt() {
        return AccrualAmt;
    }
    /**
     * @param accrualAmt the accrualAmt to set
     */
    public void setAccrualAmt(List<TField> accrualAmt) {
        AccrualAmt = accrualAmt;
    }
  
    /**
     * @return the valuesList
     */
    public List<TField> getValuesList() {
        return ValuesList;
    }
    /**
     * @param valuesList the valuesList to set
     */
    public void setValuesList(List<TField> valuesList) {
        ValuesList = valuesList;
    }



    String ParamNames;
    List<TField> FromDate;
    List<TField> ToDates;
    List<TField> NoDays;
    String KeyName;
    List<TField> AccrualAmt;
    List<TField> ValuesList;
    AaInterestAccrualsRecord accrualRec ;
    
    List<String> FromDtList = new ArrayList<String>();
    List<String> ToDtList = new ArrayList<String>();
    List<String> AccrAmtList =new ArrayList<String>();
    List<String> noDaysList =new ArrayList<String>();
    List<AccrualAmtClass> accamtclass;
    TField nodaystf;
    TField AccAmt;
    List<String> nodaystemp;
    TField todatetf ;
    TField fromdatetf;
    /**
     * @return the fromDtList
     */
    public List<String> getFromDtList() {
        return FromDtList;
    }
    /**
     * @param fromDtList the fromDtList to set
     */
    public void setFromDtList(List<String> fromDtList) {
        FromDtList = fromDtList;
    }
    /**
     * @return the toDtList
     */
    public List<String> getToDtList() {
        return ToDtList;
    }
    /**
     * @param toDtList the toDtList to set
     */
    public void setToDtList(List<String> toDtList) {
        ToDtList = toDtList;
    }
    /**
     * @return the noDaysList
     */
    public List<String> getNoDaysList() {
        return noDaysList;
    }
    /**
     * @param noDaysList the noDaysList to set
     */
    public void setNoDaysList(List<String> noDaysList) {
        this.noDaysList = noDaysList;
    }
    /**
     * @return the accrualRec
     */
    public AaInterestAccrualsRecord getAccrualRec() {
        return accrualRec;
    }
    /**
     * @param accrualRec the accrualRec to set
     */
    public void setAccrualRec(AaInterestAccrualsRecord accrualRec) {
        this.accrualRec = accrualRec;
    }
  
    public void getAccDet() {
        
        accrualRec = this.getAccrualRec();
        List<FromDateClass> FromDate = accrualRec.getFromDate();
        for (FromDateClass frdtfld : FromDate) {
        accamtclass = frdtfld.getAccrualAmt()   ;
        nodaystf = frdtfld.getDays();
        nodaystemp.add(nodaystf.toString());
        
        } 
        setNoDaysList(nodaystemp);
        
        
        for (AccrualAmtClass Amtindiv : accamtclass ) {
              
              TField AccAmt = Amtindiv.getAccrualAmt();
              
              AccrAmtList.add(AccAmt.toString());
             // paramMap.put(param.getParamName().toString(),AccAmt);
           }
    }

    
    
    
    
    
        
}
