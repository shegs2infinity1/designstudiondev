package com.cbn.bloomberg.fx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Title: CsdBloombergProperties.java Author: CSD Development Team Date Created:
 * 2025-10-27
 * 
 * Purpose: Centralized configuration loader for Bloomberg integration. Loads
 * properties from bloomberg.properties file located in TAFJ_HOME/conf or falls
 * back to classpath.
 * 
 * Usage: CsdBloombergProperties props = CsdBloombergProperties.getInstance();
 * String host = props.getMqHost();
 * 
 * Modification Details: ---- 27/10/25 - Initial version for centralized MQ
 * configuration management ---- 07/11/25 - Added FX module configuration
 * support with variable resolution
 */
public final class CsdBloombergProperties {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergProperties.class.getName());
    private static final String PROPERTIES_FILE = "bloomberg.properties";
    private static final String PLACEHOLDER_START = "${";
    private static final String PLACEHOLDER_END = "}";
    private static final String PLACEHOLDER_PAT = "${";

    private static CsdBloombergProperties instance;
    private final Properties properties;

    /**
     * Private constructor - loads properties from file system or classpath.
     */
    private CsdBloombergProperties() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Gets singleton instance of properties loader.
     */
    public static synchronized CsdBloombergProperties getInstance() {
        if (instance == null) {
            instance = new CsdBloombergProperties();
        }
        return instance;
    }

    /**
     * Loads properties from multiple locations in order of priority: 1.
     * TAFJ_HOME/conf/bloomberg.properties 2.
     * D:/Temenos/R24/bnk/UD/BLOOMBERG/conf/bloomberg.properties 3. Classpath
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
                LOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded from TAFJ_HOME: {0}", tafjPath);
            }
        }

        // Try Bloomberg UD directory
        if (!loaded) {
            Path udPath = Paths.get("/t24app/app/bnk/UD/BLOOMBERG/conf", PROPERTIES_FILE);
            if (loadFromFile(udPath)) {
                loaded = true;
                LOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded from UD directory: {0}", udPath);
            }
        }

        // Try classpath
        if (!loaded && loadFromClasspath()) {
            loaded = true;
            LOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded from classpath: {0}", PROPERTIES_FILE);
        }

        if (!loaded) {
            LOGGER.log(Level.WARNING, "[CsdBloombergProperties] Could not load {0}, using defaults", PROPERTIES_FILE);
            setDefaults();
        }
    }

    public void logLoadedProperties() {
        LOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded properties: {0}", properties);
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
            LOGGER.log(Level.WARNING, String.format("[CsdBloombergProperties] Error loading from file: %s", path), e);
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
            LOGGER.log(Level.WARNING, "[CsdBloombergProperties] Error loading from classpath", e);
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

        // FX defaults
        properties.setProperty("fx.adapter.mode", "WMQ");
        properties.setProperty("fx.file.pattern", "*.json");
        properties.setProperty("bloomberg.base.dir", "/t24app/app/bnk/UD/BLOOMBERG");
        properties.setProperty("fx.inbound.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}/IN/FX");
        properties.setProperty("fx.outbound.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}/OUT/FX");
        properties.setProperty("fx.done.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}/DONE/FX");
        properties.setProperty("fx.error.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}/ERROR/FX");
        properties.setProperty("fx.mq.inbound.queue", "FX.INBOUND.QUEUE");
        properties.setProperty("fx.mq.outbound.queue", "FX.OUTBOUND.QUEUE");

        properties.setProperty("fx.ofs.version.df", "FOREX,FX.RAD");
        properties.setProperty("fx.ofs.version.sp", "FOREX,SPOTDEAL");
        properties.setProperty("fx.ofs.version.sw", "FOREX,FX.SWAP");
        properties.setProperty("fx.ofs.version.fw", "FOREX,FORWARDDEAL");
        properties.setProperty("fx.ofs.source", "OFS.BMRG");
        properties.setProperty("fx.ofs.function", "INPUT");
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
    // ==== FX MODULE PROPERTIES GETTERS ====
    // ====

    public String getFxAdapterMode() {
        return properties.getProperty("fx.adapter.mode", "WMQ");
    }

    public String getFxFilePattern() {
        return properties.getProperty("fx.file.pattern", "*.json");
    }

    public String geFxtInboundDir() {
        return resolveValue(properties.getProperty("fx.inbound.dir", "/t24app/app/bnk/UD/BLOOMBERG/IN/FX"));
    }
    
    public String geFxtOutboundDir() {
        return resolveValue(properties.getProperty("fx.outbound.dir", "/t24app/app/bnk/UD/BLOOMBERG/OUT/FX"));
    }

    public String getFxDoneDir() {
        return resolveValue(properties.getProperty("fx.done.dir", "/t24app/app/bnk/UD/BLOOMBERG/DONE/FX"));
    }

    public String getFxErrorDir() {
        return resolveValue(properties.getProperty("fx.error.dir", "/t24app/app/bnk/UD/BLOOMBERG/ERROR/FX"));
    }

    public String getFxMqInboundQueue() {
        return properties.getProperty("fx.mq.inbound.queue", "FX.INBOUND.QUEUE");
    }

    public String getFxMqOutboundQueue() {
        return properties.getProperty("fx.mq.outbound.queue", "FX.OUTBOUND.QUEUE");
    }

    public String getFxOfsSource() {
        return properties.getProperty("fx.ofs.source", "OFS.LOAD");
    }

    public String getFxOfsFunction() {
        return properties.getProperty("fx.ofs.function", "INPUT");
    }

    public String getFxOfsVersionDefault() {
        return properties.getProperty("fx.ofs.version.df", "FOREX,RAD");
    }

    public String getFxOfsVersionForDealType(String dealType) {
        if (dealType == null || dealType.trim().isEmpty()) {
            return getFxOfsVersionDefault();
        }
        String key = "fx.ofs.version." + dealType.trim().toLowerCase();
        LOGGER.log(Level.INFO, "[CsdBloombergProperties] Fx Transaction Version: {0}", key);
        return properties.getProperty(key, getFxOfsVersionDefault());
    }

    // ====
    // ==== UTILITY METHODS ====
    // ====

    /**
     * Resolves variable substitution in property values. Supports ${property.name}
     * syntax.
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