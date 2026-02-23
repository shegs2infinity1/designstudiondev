package com.mcbc.tmb.cusdelta;

import java.util.List;

/**
 * TODO: Document me! Class for Rollover Dates 
 *
 * @author debdas
 *
 */
public class SortDates {
    
    List<String> RollDates;
    String MaxDate;
    String MinDate;

    /**
     * @return the minDate
     */
    public String getMinDate() {
        return MinDate;
    }

    /**
     * @param minDate the minDate to set
     */
    public void setMinDate(String minDate) {
        MinDate = minDate;
    }

    /**
     * @return the rollDates
     */
    public List<String> getRollDates() {
        return RollDates;
    }

    /**
     * @return the maxDate
     */
    public String getMaxDate() {
        return MaxDate;
    }

    /**
     * @param maxDate the maxDate to set
     */
    public void setMaxDate(String maxDate) {
        MaxDate = maxDate;
    }

    /**
     * @param rollDates the rollDates to set
     */
    public void setRollDates(List<String> rollDates) {
        RollDates = rollDates;
    }
    
    
    
    
    void GetLatestDates()
    {
        
        List<String> TempDates = getRollDates();
        int MaxValue = 0;
        for (String TempDt : TempDates)
        {
         if (Integer.parseInt(TempDt)>MaxValue ) {
             MaxValue = Integer.parseInt(TempDt);
         }
             
            
        }
        
        setMaxDate(Integer.toString(MaxValue));
    }
    
    void GetEarliestDate() {
        
        
          
            List<String> TempDates = getRollDates();
            String firstValue = TempDates.get(0);
            int MinValue = Integer.parseInt(firstValue);
            for (String TempDt : TempDates)
            {
             if (Integer.parseInt(TempDt)< MinValue ) {
                 MinValue = Integer.parseInt(TempDt);
             }
                 
                
            }
            
            setMinDate(Integer.toString(MinValue));
        
    }

}
