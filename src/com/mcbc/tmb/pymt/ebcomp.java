package com.mcbc.tmb.pymt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.temenos.api.TField;
import com.temenos.t24.api.records.ebcommonparamtmb.EbCommonParamTmbRecord;
import com.temenos.t24.api.records.ebcommonparamtmb.ParamNameClass;

/**
 * TODO: Document me!
 *
 * @author Debesh
 *
 */
public class ebcomp {
    
    
 EbCommonParamTmbRecord ecprec   ;
 
 Map<String,List<TField>> paramMap = new HashMap <String,List<TField>>();
 String ParamNames;
 List<TField> ParamValues;
 List<TField> ValuesList;
 String KeyName;
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
 * @return the paramValues
 */
public List<TField> getParamValues() {
    return ParamValues;
}


/**
 * @param paramValues the paramValues to set
 */
public void setParamValues(List<TField> paramValues) {
    ParamValues = paramValues;
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

  /**
 * @return the ecprec
 */
public EbCommonParamTmbRecord getEcprec() {
    return ecprec;
}


/**
 * @param ecprec the ecprec to set
 */
public void setEcprec(EbCommonParamTmbRecord ecprec) {
    this.ecprec = ecprec;
}

public void getComp(){
    ecprec = this.getEcprec();
List<ParamNameClass> ParamName = ecprec.getParamName();
for (ParamNameClass param : ParamName){
   System.out.println(param.toString());
   System.out.println("param Names in class" + param.getParamName().toString() );
   System.out.println("param Values in class " + param.getParamValue().toString());
   paramMap.put(param.getParamName().toString(),param.getParamValue());
   this.setValuesList(paramMap.get(this.getKeyName()));
   
   
} 



}



}