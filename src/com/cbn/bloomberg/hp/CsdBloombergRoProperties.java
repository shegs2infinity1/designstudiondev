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
 * Title: CsdBloombergProperties.java Author: CSD Development Team Date Created: 2025-10-27
 *
 * Purpose: Centralized configuration loader for Bloomberg integration. Loads properties from
 * bloomberg.properties file located in TAFJ_HOME/conf or falls back to classpath.
 *
 * Usage: CsdBloombergProperties props = CsdBloombergProperties.getInstance(); String host =
 * props.getMqHost();
 *
 * Modification Details: ---- 27/10/25 - Initial version for centralized MQ configuration management
 * ---- 07/11/25 - Added RO module configuration support with variable resolution
 */
public final class CsdBloombergRoProperties {

  private static final Logger yLOGGER = Logger.getLogger(CsdBloombergRoProperties.class.getName());
  private static final String PROPERTIES_FILE = "bloombergro.properties";
  private static final String PLACEHOLDER_START = "${";
  private static final String PLACEHOLDER_END = "}";
  private static final String PLACEHOLDER_PAT = "${";
  private static CsdBloombergRoProperties instance;
  private final Properties properties;

  /**
   * Private constructor - loads properties from file system or classpath.
   */
  private CsdBloombergRoProperties() {
    this.properties = new Properties();
    loadProperties();
  }

  /**
   * Gets singleton instance of properties loader.
   */
  public static synchronized CsdBloombergRoProperties getInstance() {
    if (instance == null) {
      instance = new CsdBloombergRoProperties();
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
        yLOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded from TAFJ_HOME: {0}", tafjPath);
      }
    }

    // Try Bloomberg UD directory
    if (!loaded) {
      Path udPath = Paths.get("D:", "Temenos", "R24", "bnk", "UD", "BLOOMBERG", "conf",
          PROPERTIES_FILE);
      if (loadFromFile(udPath)) {
        loaded = true;
        yLOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded from UD directory: {0}", udPath);
      }
    }

    // Try classpath
    if (!loaded && loadFromClasspath()) {
      loaded = true;
      yLOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded from classpath: {0}",
          PROPERTIES_FILE);
    }

    if (!loaded) {
      yLOGGER.log(Level.WARNING, "[CsdBloombergProperties] Could not load {0}, using defaults",
          PROPERTIES_FILE);
      setDefaults();
    }
  }

  public void logLoadedProperties() {
    yLOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded properties: {0}", properties);
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
          String.format("[CsdBloombergProperties] Error loading from file: %s", path), e);
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
      yLOGGER.log(Level.WARNING, "[CsdBloombergProperties] Error loading from classpath", e);
    }
    return false;
  }

  /**
   * Sets default properties if file cannot be loaded.
   */

  private void setDefaults() {
    properties.setProperty("mq.host", "172.105.249.157");
    properties.setProperty("mq.port", "1414");
    properties.setProperty("mq.channel", "DEV.APP.SVRCONN");
    properties.setProperty("mq.qmgr", "QM_BLOOMBERG");
    properties.setProperty("mq.user", "");
    properties.setProperty("mq.password", "");
    properties.setProperty("mq.ack", "auto");

    // RO defaults
    properties.setProperty("ro.adapter.mode", "WMQ");
    properties.setProperty("ro.file.pattern", "*.json");
    properties.setProperty("bloomberg.base.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG");
    properties.setProperty("ro.inbound.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\IN\\RO");
    properties.setProperty("ro.outbound.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\OUT\\RO");
    properties.setProperty("ro.done.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\DONE\\RO");
    properties.setProperty("ro.error.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\ERROR\\RO");
    properties.setProperty("ro.mq.inbound.queue", "RO.INBOUND.QUEUE");
    properties.setProperty("ro.mq.outbound.queue", "RO.OUTBOUND.QUEUE");

    properties.setProperty("ro.ofs.version.df", "FUNDS.TRANSFER,AC");
    properties.setProperty("ro.ofs.version.ft", "FUNDS.TRANSFER,CBN.BKSD.FMD");
    properties.setProperty("ro.ofs.source", "OFS.BMRG");
    properties.setProperty("ro.ofs.function", "INPUT");
  }

  // ====
  // ==== MQ PROPERTIES GETTERS ====\
  // ====

  public String getMqHost() {
    return properties.getProperty("mq.host", "172.105.249.157");
  }

  public int getMqPort() {
    return Integer.parseInt(properties.getProperty("mq.port", "1414"));
  }

  public String getMqChannel() {
    return properties.getProperty("mq.channel", "DEV.APP.SVRCONN");
  }

  public String getMqQueueManager() {
    return properties.getProperty("mq.qmgr", "QM_BLOOMBERG");
  }

  public String getMqUser() {
    return properties.getProperty("mq.user", "");
  }

  public String getMqPassword() {
    return properties.getProperty("mq.password", "");
  }

  public String getMqAckMode() {
    return properties.getProperty("mq.ack", "auto");
  }

  // ====
  // ==== RO MODULE PROPERTIES GETTERS ====
  // ====

  public String getRoAdapterMode() {
    return properties.getProperty("ro.adapter.mode", "WMQ");
  }

  public String getRoFilePattern() {
    return properties.getProperty("ro.file.pattern", "*.json");
  }

  public String getRoInboundDir() {
    return resolveValue(
        properties.getProperty("ro.inbound.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\IN\\RO"));
  }

  public String getRoOutboundDir() {
    return resolveValue(
        properties.getProperty("ro.inbound.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\OUT\\RO"));
  }

  public String getRoDoneDir() {
    return resolveValue(
        properties.getProperty("ro.done.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\DONE\\RO"));
  }

  public String getRoErrorDir() {
    return resolveValue(
        properties.getProperty("ro.error.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\ERROR\\RO"));
  }

  public String getRoMqInboundQueue() {
    return properties.getProperty("ro.mq.inbound.queue", "RO.INBOUND.QUEUE");
  }

  public String getRoMqOutboundQueue() {
    return properties.getProperty("ro.mq.outbound.queue", "RO.OUTBOUND.QUEUE");
  }

  public String getRoOfsSource() {
    return properties.getProperty("ro.ofs.source", "OFS.BRMG");
  }

  public String getRoOfsFunction() {
    return properties.getProperty("ro.ofs.function", "INPUT");
  }

  public String getRoOfsVersionDef() {
    return properties.getProperty("ro.ofs.version.df", "EB.CBN.REPO.PLACEMENT,CBN.REPO");
  }

  public String getRoOfsVersion() {
    return properties.getProperty("ro.ofs.version.ft", getRoOfsVersionDef());
  }

  // ====
  // ==== UTILITY METHODS ====
  // ====\

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