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
 * Title: CsdBloombergFxProperties.java
 * 
 * @author Muanqee
 * 
 * @created: 2025-10-27
 * 
 * @purpose: Centralized configuration loader for Bloomberg integration. Loads properties from
 *           bloomberg.properties file located in TAFJ_HOME/conf or falls back to classpath.
 * 
 * @usage: CsdBloombergProperties
 * 
 *         props = CsdBloombergProperties.getInstance();
 * 
 *         String host = props.getMqHost();
 * 
 * @modification Details:
 * 
 *               27/10/25 - Initial version for centralized MQ configuration management
 * 
 *               07/11/25 - Added FX module configuration support with variable resolution
 * 
 *               11/01/26 - Added new operation to process FX Deals Local App
 */
public final class CsdBloombergFxProperties {

    private static final Logger yLogger = Logger.getLogger(
            CsdBloombergFxProperties.class.getName());
    private static final String PROPERTIES_FILE = "fx-bloomberg.properties";
    private static final String PLACEHOLDER_START = "${";
    private static final String PLACEHOLDER_CLOSE = "}";
    private static final String PLACEHOLDER_PATTN = "${";

    private static CsdBloombergFxProperties instance;
    private final Properties properties;

    /**
     * Private constructor - loads properties from file system or classpath.
     */
    private CsdBloombergFxProperties() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Gets singleton instance of properties loader.
     */
    public static synchronized CsdBloombergFxProperties getInstance() {
        if (instance == null) {
            instance = new CsdBloombergFxProperties();
        }
        return instance;
    }

    /**
     * Loads properties from multiple locations in order of priority: 1.
     * TAFJ_HOME/conf/bloomberg.properties 2.
     * D:/Temenos/R24/bnk/UD/BLOOMBERG/conf/bloomberg.properties 3. Classpath (bloomberg.properties)
     */
    private void loadProperties() {
        boolean loaded = false;

        // Try TAFJ_HOME/conf first
        String tafj = System.getenv("TAFJ_HOME");
        if (tafj != null && !tafj.isEmpty()) {
            Path tafjPath = Paths.get(tafj, "conf", PROPERTIES_FILE);
            if (loadFromFile(tafjPath)) {
                loaded = true;
                yLogger.log(Level.INFO, "[CsdBloombergFxProperties] Loaded from TAFJ_HOME: {0}",
                        tafjPath);
            }
        }

        // Try Bloomberg UD directory
        if (!loaded) {
            Path udPath = Paths.get("D:", "Temenos", "R24", "bnk", "UD", "BLOOMBERG", "conf",
                    PROPERTIES_FILE);
            if (loadFromFile(udPath)) {
                loaded = true;
                yLogger.log(Level.INFO, "[CsdBloombergFxProperties] Loaded from UD directory: {0}",
                        udPath);
            }
        }

        // Try classpath
        if (!loaded && loadFromClasspath()) {
            loaded = true;
            yLogger.log(Level.INFO, "[CsdBloombergFxProperties] Loaded from classpath: {0}",
                    PROPERTIES_FILE);
        }

        if (!loaded) {
            yLogger.log(Level.WARNING,
                    "[CsdBloombergFxProperties] Could not load {0}, using defaults",
                    PROPERTIES_FILE);
            setDefaults();
        }
    }

    public void logLoadedProperties() {
        yLogger.log(Level.INFO, "[CsdBloombergFxProperties] Loaded properties: {0}", properties);
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
            yLogger.log(Level.WARNING,
                    String.format("[CsdBloombergFxProperties] Error loading from file: %s", path),
                    e);
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
            yLogger.log(Level.WARNING, "[CsdBloombergFxProperties] Error loading from classpath",
                    e);
        }
        return false;
    }

    /**
     * Sets default properties if file cannot be loaded.
     */

    private void setDefaults() {

        // FX OFS Options Defaults
        properties.setProperty("fx.ofs.adapter", "WMQ");
        properties.setProperty("fx.ofs.source", "OFS.BMRG");
        properties.setProperty("fx.ofs.function", "INPUT");
        properties.setProperty("fx.ofs.version.df", "FOREX,FX.RAD");
        properties.setProperty("fx.ofs.version.st", "FOREX,SPOTDEAL");
        properties.setProperty("fx.ofs.version.sp", "FOREX,FX.SWAP");
        properties.setProperty("fx.ofs.version.fw", "FOREX,FORWARDDEAL");
        properties.setProperty("fx.ofs.version.dt", "FX.CBN.BKSD.REQUEST,RAD");
        properties.setProperty("fx.ofs.version.sd", "FX.CBN.BKSD.REQUEST,BKSD.REQ");

        // FX WMQ Node Defaults
        properties.setProperty("fx.wmq.host", "172.22.105.46");
        properties.setProperty("fx.wmq.port", "1414");
        properties.setProperty("fx.wmq.channel", "DEV.APP.SVRCONN");
        properties.setProperty("fx.wmq.manager", "QM_BLOOMBERG");
        properties.setProperty("fx.wmq.username", "");
        properties.setProperty("fx.wmq.password", "");
        properties.setProperty("fx.wmq.ackledge", "auto");
        properties.setProperty("fx.wmq.inbound.queue", "FX.INBOUND.QUEUE");
        properties.setProperty("fx.wmq.outbound.queue", "FX.OUTBOUND.QUEUE");

        // FX File Node defaults
        properties.setProperty("fx.nfs.file.pattern", "*.json");
        properties.setProperty("fx.nfs.bloomberg.base.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG");
        properties.setProperty("fx.nfs.inbound.dir",
                PLACEHOLDER_PATTN + "fx.nfs.bloomberg.base.dir}\\IN\\FX");
        properties.setProperty("fx.nfs.outbound.dir",
                PLACEHOLDER_PATTN + "fx.nfs.bloomberg.base.dir}\\OUT\\FX");
        properties.setProperty("fx.nfs.done.dir",
                PLACEHOLDER_PATTN + "fx.nfs.bloomberg.base.dir}\\DONE\\FX");
        properties.setProperty("fx.nfs.error.dir",
                PLACEHOLDER_PATTN + "fx.nfs.bloomberg.base.dir}\\ERROR\\FX");
    }

    // ====
    // ==== OFS Adapter PROPERTIES GETTERS ====\
    // ====
    public String getOfsAdapter() {
        return properties.getProperty("fx.ofs.adapter", "WMQ");
    }

    public String getOfsSource() {
        return properties.getProperty("fx.ofs.source", "OFS.LOAD");
    }

    public String getOfsFunction() {
        return properties.getProperty("fx.ofs.function", "INPUT");
    }

    public String getOfsVersionDefault() {
        return properties.getProperty("fx.ofs.version.df", "FOREX,RAD");
    }

    public String getOfsVersionDealType(String dealType) {
        if (dealType == null || dealType.trim().isEmpty()) {
            return getOfsVersionDefault();
        }
        String key = "fx.ofs.version." + dealType.trim().toLowerCase();
        yLogger.log(Level.INFO, "[CsdBloombergFxProperties] Fx Transaction Version: {0}", key);
        return properties.getProperty(key, getOfsVersionDefault());
    }

    // ====
    // ==== MQ PROPERTIES GETTERS ====\
    // ====

    public String getMqHost() {
        return properties.getProperty("fx.wmq.host", "172.22.105.46");
    }

    public int getMqPort() {
        return Integer.parseInt(properties.getProperty("fx.wmq.port", "1414"));
    }

    public String getMqChannel() {
        return properties.getProperty("fx.wmq.channel", "DEV.APP.SVRCONN");
    }

    public String getMqQueueManager() {
        return properties.getProperty("fx.wmq.manager", "QM_BLOOMBERG");
    }

    public String getMqUser() {
        return properties.getProperty("fx.wmq.username", "");
    }

    public String getMqPassword() {
        return properties.getProperty("fx.wmq.password", "");
    }

    public String getMqAckMode() {
        return properties.getProperty("fx.wmq.ackledge", "auto");
    }

    public String getMqInboundQueue() {
        return properties.getProperty("fx.wmq.inbound.queue", "FX.INBOUND.QUEUE");
    }

    public String getMqOutboundQueue() {
        return properties.getProperty("fx.wmq.outbound.queue", "FX.OUTBOUND.QUEUE");
    }

    // ====
    // ==== FX MODULE PROPERTIES GETTERS ====
    // ====

    public String getFsFilePattern() {
        return properties.getProperty("fx.nfs.file.pattern", "*.json");
    }

    public String getFsInboundDir() {
        return resolveValue(properties.getProperty("fx.nfs.inbound.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\IN\\FX"));
    }

    public String getFsOutboundDir() {
        return resolveValue(properties.getProperty("fx.nfs.inbound.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\OUT\\FX"));
    }

    public String getFsDoneDir() {
        return resolveValue(properties.getProperty("fx.nfs.done.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\DONE\\FX"));
    }

    public String getFsErrorDir() {
        return resolveValue(properties.getProperty("fx.nfs.error.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\ERROR\\FX"));
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
            int end = result.indexOf(PLACEHOLDER_CLOSE, start);

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