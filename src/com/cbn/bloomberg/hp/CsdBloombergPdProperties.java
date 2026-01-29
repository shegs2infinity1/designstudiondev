package com.cbn.bloomberg.hp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Title: CsdBloombergPdProperties.java.
 * 
 * Author: CSD Development Team Date Created: 2026-01-06
 *
 * Purpose: Centralized configuration loader for Bloomberg integration. Loads properties from
 * bloomberg.properties file located in TAFJ_HOME/conf or falls back to classpath.
 *
 * Usage: CsdBloombergPdProperties props = CsdBloombergPdProperties.getInstance(); String host =
 * props.getMqHost();
 *
 * Modification Details:
 * 
 * ----06/01/25 - Initial version for centralized MQ configuration management
 * 
 * ----07/01/25 - 06/01/25 Added PD module configuration support with variable resolution
 */
public final class CsdBloombergPdProperties {

   private static final Logger yLOGGER = Logger.getLogger(CsdBloombergPdProperties.class.getName());
   private static final String PROPERTIES_FILE = "pd-bloomberg.properties";
   private static final String PLACEHOLDER_START = "${";
   private static final String PLACEHOLDER_END = "}";
   private static final String PLACEHOLDER_PAT = "${";
   private static CsdBloombergPdProperties instance;
   private final Properties properties;

   /**
    * Private constructor - loads properties from file system or classpath.
    */
   private CsdBloombergPdProperties() {
      this.properties = new Properties();
      loadProperties();
   }

   /**
    * Gets singleton instance of properties loader.
    */
   public static synchronized CsdBloombergPdProperties getInstance() {
      if (instance == null) {
         instance = new CsdBloombergPdProperties();
      }
      return instance;
   }

   /**
    * Loads properties from multiple locations in order of priority: 1.
    * TAFJ_HOME/conf/bloomberg.properties 2.
    * D:/Temenos/R24/bnk/UD/BLOOMBERG/resources/bloomberg.properties 3. Classpath
    * (bloomberg.properties)
    */

   private void loadProperties() {
      boolean loaded = false;

      // Try TAFJ_HOME/conf first
      String tafj = System.getenv("TAFJ_HOME");
      if (tafj != null && !tafj.isEmpty()) {
         Path tafjPath = Paths.get(tafj, "conf", PROPERTIES_FILE);
         if (loadFromFile(tafjPath)) {
            loaded = true;
            yLOGGER.log(Level.INFO, "[CsdBloombergPdProperties] Loaded from TAFJ_HOME: {0}",
                  tafjPath);
         }
      }

      // Try Bloomberg UD directory
      if (!loaded) {
         Path udPath = Paths.get("D:", "Temenos", "R24", "bnk", "UD", "BLOOMBERG", "conf",
               PROPERTIES_FILE);
         if (loadFromFile(udPath)) {
            loaded = true;
            yLOGGER.log(Level.INFO, "[CsdBloombergPdProperties] Loaded from UD directory: {0}",
                  udPath);
         }
      }

      // Try classpath
      if (!loaded && loadFromClasspath()) {
         loaded = true;
         yLOGGER.log(Level.INFO, "[CsdBloombergPdProperties] Loaded from classpath: {0}",
               PROPERTIES_FILE);
      }

      if (!loaded) {
         yLOGGER.log(Level.WARNING, "[CsdBloombergPdProperties] Could not load {0}, using defaults",
               PROPERTIES_FILE);
         setDefaults();
      }
   }

   public void logLoadedProperties() {
      yLOGGER.log(Level.INFO, "[CsdBloombergPdProperties] Loaded properties: {0}", properties);
   }

   /**
    * Loads properties from file system path.
    */
   private boolean loadFromFile(Path path) {
      if (!Files.exists(path)) {
         return false;
      }
      try (InputStream is = Files.newInputStream(path)) {
         properties.load(is);
         return true;
      } catch (IOException e) {
         yLOGGER.log(Level.WARNING,
               String.format("[CsdBloombergPdProperties] Error loading from file: %s", path), e);
         return false;
      }
   }

   /**
    * Loads properties from classpath.
    */
   private boolean loadFromClasspath() {
      try (InputStream is = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
         if (is != null) {
            properties.load(is);
            return true;
         }
      } catch (IOException e) {
         yLOGGER.log(Level.WARNING, "[CsdBloombergPdProperties] Error loading from classpath", e);
      }
      return false;
   }

   /**
    * Sets default properties if file cannot be loaded.
    */

   private void setDefaults() {
      // Adapter Run Mode Defaults
      properties.setProperty("pd.ofs.adapter", "WMQ");
      properties.setProperty("pd.ofs.function", "INPUT");
      properties.setProperty("pd.ofs.source", "OFS.BMRG");
      properties.setProperty("pd.ofs.version.def", "EB.CBN.PLACEMENT.DEPOSIT,RAD");
      properties.setProperty("pd.ofs.version.cbn", "EB.CBN.PLACEMENT.DEPOSIT,CBN");

      // PD defaults - WMQ Mode
      properties.setProperty("pd.mq.host", "172.22.105.46");
      properties.setProperty("pd.mq.port", "1414");
      properties.setProperty("pd.mq.channel", "DEV.APP.SVRCONN");
      properties.setProperty("pd.mq.qmgr", "QM_BLOOMBERG");
      properties.setProperty("pd.mq.user", "");
      properties.setProperty("pd.mq.password", "");
      properties.setProperty("pd.mq.ack", "auto");
      properties.setProperty("pd.mq.inbound.queue", "PD.INBOUND.QUEUE");
      properties.setProperty("pd.mq.outbound.queue", "PD.OUTBOUND.QUEUE");

      // PD defaults - File Mode
      properties.setProperty("pd.fs.bloomberg.base.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG");
      properties.setProperty("pd.fs.inbound.dir",
            PLACEHOLDER_PAT + "pd.fs.bloomberg.base.dir}\\IN\\PD");
      properties.setProperty("pd.fs.outbound.dir",
            PLACEHOLDER_PAT + "pd.fs.bloomberg.base.dir}\\OUT\\PD");
      properties.setProperty("pd.fs.done.dir",
            PLACEHOLDER_PAT + "pd.fs.bloomberg.base.dir}\\DONE\\PD");
      properties.setProperty("pd.fs.error.dir",
            PLACEHOLDER_PAT + "pd.fs.bloomberg.base.dir}\\ERROR\\PD");
      properties.setProperty("pd.fs.file.pattern", "*.json");
   }

   // ====
   // ==== OFS Adapter PROPERTIES GETTERS ====
   // ====

   public String getAdapterMode() {
      return properties.getProperty("pd.ofs.adapter", "WMQ");
   }

   public String getOfsSource() {
      return properties.getProperty("pd.ofs.source", "OFS.BRMG");
   }

   public String getOfsFunction() {
      return properties.getProperty("pd.ofs.function", "INPUT");
   }

   public String getOfsVersionDef() {
      return properties.getProperty("pd.ofs.version.def", "EB.CBN.PLACEMENT.DEPOSIT,RAD");
   }

   public String getOfsVersion() {
      return properties.getProperty("pd.ofs.version.cbn", getOfsVersionDef());
   }

   // ====
   // ==== MQ Adapter PROPERTIES GETTERS ====
   // ====

   public String getMqHost() {
      return properties.getProperty("pd.mq.host", "172.22.105.46");
   }

   public int getMqPort() {
      return Integer.parseInt(properties.getProperty("pd.mq.port", "1414"));
   }

   public String getMqChannel() {
      return properties.getProperty("pd.mq.channel", "DEV.APP.SVRCONN");
   }

   public String getMqQueueManager() {
      return properties.getProperty("pd.mq.qmgr", "QM_BLOOMBERG");
   }

   public String getMqUser() {
      return properties.getProperty("pd.mq.user", "");
   }

   public String getMqPassword() {
      return properties.getProperty("pd.mq.password", "");
   }

   public String getMqAckMode() {
      return properties.getProperty("pd.mq.ack", "auto");
   }

   public String getMqInboundQueue() {
      return properties.getProperty("pd.mq.inbound.queue", "PD.INBOUND.QUEUE");
   }

   public String getMqOutboundQueue() {
      return properties.getProperty("pd.mq.outbound.queue", "PD.OUTBOUND.QUEUE");
   }

   // ====
   // ==== File Adapter PROPERTIES GETTERS ====
   // ====

   public String getFsInboundDir() {
      return resolveValue(properties.getProperty("pd.fs.inbound.dir",
            "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\IN\\PD"));
   }

   public String getFsOutboundDir() {
      return resolveValue(properties.getProperty("pd.fs.outbound.dir",
            "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\OUT\\PD"));
   }

   public String getFsDoneDir() {
      return resolveValue(properties.getProperty("pd.fs.done.dir",
            "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\DONE\\PD"));
   }

   public String getFsErrorDir() {
      return resolveValue(properties.getProperty("pd.fs.error.dir",
            "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\ERROR\\PD"));
   }

   public String getFsFilePattern() {
      return properties.getProperty("pd.fs.file.pattern", "*.json");
   }

   // ====
   // ==== UTILITY METHODS ====
   // ====

   /**
    * Resolves variable substitution in property values. Supports ${property.name} syntax.
    */
   private String resolveValue(String value) {
      if (value == null || !value.contains(PLACEHOLDER_START)) {
         return value;
      }

      String result = value;
      int maxIterations = 10;
      int iteration = 0;

      while (result.contains(PLACEHOLDER_START) && iteration < maxIterations) {
         int start = result.indexOf(PLACEHOLDER_START);
         int end = result.indexOf(PLACEHOLDER_END, start);

         if (end > start) {
            String varName = result.substring(start + PLACEHOLDER_START.length(), end);
            String varValue = properties.getProperty(varName, "");
            result = result.substring(0, start) + varValue + result.substring(end + 1);
         }
         iteration++;
      }

      return result;
   }

   /**
    * Gets raw properties object for advanced usage.
    */
   public Properties getProperties() {
      return (Properties) properties.clone();
   }

   /**
    * Gets a property by key with optional default value.
    */
   public String getProperty(String key, String defaultValue) {
      return properties.getProperty(key, defaultValue);
   }

   /**
    * Gets a property by key.
    */
   public String getProperty(String key) {
      return properties.getProperty(key);
   }
}