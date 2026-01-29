package com.mcbc.tmb.cusdelta;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
/**
 * TODO: Document me!
 *
 * @author Debesh
 * - Some Utility with T24 Dates , getting no of days between 2 dates
 */
public class T24Date {
    
    String DateFormat;
    String FirstDate;
    String EndDate;
    long noDays;
   
    
    /**
     * @return the noDays
     */
    public long getNoDays() {
        return noDays;
    }
    /**
     * @param noDays the noDays to set
     */
    public void setNoDays(long noDays) {
        this.noDays = noDays;
    }
    /**
     * @return the dateFormat
     */
    public String getDateFormat() {
        return DateFormat;
    }
    /**
     * @param dateFormat the dateFormat to set
     */
    public void setDateFormat(String dateFormat) {
        DateFormat = dateFormat.substring(0, 4)+"-"+dateFormat.substring(4,6)+ "-"+ dateFormat.substring(6,8);
    }
    /**
     * @return the firstDate
     */
    public String getFirstDate() {
        return FirstDate;
    }
    /**
     * @param firstDate the firstDate to set
     */
    public void setFirstDate(String firstDate) {
        FirstDate = firstDate;
    }
    /**
     * @return the endDate
     */
    public String getEndDate() {
        return EndDate;
    }
    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(String endDate) {
        EndDate = endDate;
    }
    
    public void getDiffdays() {
        
        String Day1;
        String Day2;
        Day1 = this.getFirstDate();
        Day2 = this.getEndDate();
        this.setDateFormat(Day1);
        String ConvDay1 = getDateFormat();
        this.setDateFormat(Day2);
        String ConvDay2 = getDateFormat();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        try {
            
            LocalDate date1 = LocalDate.parse(ConvDay1, formatter);
            LocalDate date2 = LocalDate.parse(ConvDay2, formatter);
            // Calculate the number of days between the two dates
            long daysBetween = ChronoUnit.DAYS.between(date1, date2);
             
            this.setNoDays(daysBetween);
            System.out.println("Days "+ daysBetween);   
                     
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            // Uncomment and replace with appropriate logger
            // LOGGER.error(e1, e1);
            System.out.println("Exception T24Date Class " + e1); 
        }   
        
        
    }

}
