package com.acquire.intelligentforms;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * TODO: Document me!
 *
 * @author shegs
 *
 */
public class License {

    
    private static final String UNLIMITED_LICENSE = "unlimited";
    
    private static final int UNLIMITED_PHASES = -1;
    
    private static int s_allowedPhases;
    
    private static int s_phasesUsed = 0;
    
    protected static Date s_expiryDate;
    
    private static boolean s_loaded = true;
    
    private static String s_domainUrls = null;
    
    private static boolean s_validateUrls = true;
    
    private static String s_errorLicenseMsg;
    
    public static void validate(File p_licenseFile, File p_keyFile) throws Exception {
           s_loaded = true;
           boolean validIP = true;
      }
    public static boolean isLoaded() {
        return s_loaded;
      }
    public static void incrementPhasesUsed(int p_used) throws Exception {
        s_phasesUsed += 1;
  
      }
    public static void validateDomainUrl(String p_currentDomainUrl) throws Exception {
        if (!s_validateUrls)
            return; 
      }
    
    public static Date getExpiryDate() throws ParseException {
        
//        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
//        s_expiryDate = fmt.parse("2034-01-01");
        return s_expiryDate;
      }
    
    private static String getDomainNameByIp() {
        String domain = "";
        return domain;
      }
    
    public static void resetPhasesUsed() {
        s_phasesUsed = 0;
      }
      
}

