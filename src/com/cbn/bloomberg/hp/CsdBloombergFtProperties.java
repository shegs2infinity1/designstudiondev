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
 * configuration management ---- 07/11/25 - Added FT module configuration
 * support with variable resolution
 */
public final class CsdBloombergFtProperties {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFtProperties.class.getName());
    private static final String PROPERTIES_FILE = "bloombergft.properties";
    private static final String PLACEHOLDER_START = "${";
    private static final String PLACEHOLDER_END = "}";
    private static final String PLACEHOLDER_PAT = "${";
    private static CsdBloombergFtProperties instance;
    private final Properties properties;

    /**
     * Private constructor - loads properties from file system or classpath.
     */
    private CsdBloombergFtProperties() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Gets singleton instance of properties loader.
     */
    public static synchronized CsdBloombergFtProperties getInstance() {
        if (instance == null) {
            instance = new CsdBloombergFtProperties();
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
                LOGGER.log(Level.INFO, "[CsdBloombergProperties] Loaded from TAFJ_HOME: {0}", tafjPath);
            }
        }

        // Try Bloomberg UD directory
        if (!loaded) {
            Path udPath = Paths.get("/t24app/app/bnk/UD/BLOOMBERG/conf/", PROPERTIES_FILE);
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

        // FT defaults
        properties.setProperty("ft.adapter.mode", "WMQ");
        properties.setProperty("ft.file.pattern", "*.json");
        properties.setProperty("bloomberg.base.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG");
        properties.setProperty("ft.inbound.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\IN\\FT");
        properties.setProperty("ft.outbound.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\OUT\\FT");
        properties.setProperty("ft.done.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\DONE\\FT");
        properties.setProperty("ft.error.dir", PLACEHOLDER_PAT + "bloomberg.base.dir}\\ERROR\\FT");
        properties.setProperty("ft.mq.inbound.queue", "FT.INBOUND.QUEUE");
        properties.setProperty("ft.mq.outbound.queue", "FT.OUTBOUND.QUEUE");

        properties.setProperty("ft.ofs.version.df", "FUNDS.TRANSFER,AC");
        properties.setProperty("ft.ofs.version.ft", "FUNDS.TRANSFER,CBN.BKSD.FMD");
        properties.setProperty("ft.ofs.source", "OFS.BMRG");
        properties.setProperty("ft.ofs.function", "INPUT");
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
    // ==== FT MODULE PROPERTIES GETTERS ====
    // ====

    public String getFtAdapterMode() {
        return properties.getProperty("ft.adapter.mode", "WMQ");
    }

    public String getFtFilePattern() {
        return properties.getProperty("ft.file.pattern", "*.json");
    }

    public String geFtInboundDir() {
        return resolveValue(properties.getProperty("ft.inbound.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\IN\\FT"));
    }

    public String geFtOutboundDir() {
        return resolveValue(properties.getProperty("ft.inbound.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\OUT\\FT"));
    }

    public String getFtDoneDir() {
        return resolveValue(properties.getProperty("ft.done.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\DONE\\FT"));
    }

    public String getFtErrorDir() {
        return resolveValue(properties.getProperty("ft.error.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\ERROR\\FT"));
    }

    public String getFtMqInboundQueue() {
        return properties.getProperty("ft.mq.inbound.queue", "FT.INBOUND.QUEUE");
    }

    public String getFtMqOutboundQueue() {
        return properties.getProperty("ft.mq.outbound.queue", "FT.OUTBOUND.QUEUE");
    }

    public String getFtOfsSource() {
        return properties.getProperty("ft.ofs.source", "OFS.LOAD");
    }

    public String getFtOfsFunction() {
        return properties.getProperty("ft.ofs.function", "INPUT");
    }

    public String getFtOfsVersionDef() {
        return properties.getProperty("ft.ofs.version.df", "FUNDS.TRANSFER,AC");
    }

    public String getFtOfsVersion() {
        return properties.getProperty("ft.ofs.version.ft", getFtOfsVersionDef());
    }

    // ====
    // ==== UTILITY METHODS ====
    // ====\

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