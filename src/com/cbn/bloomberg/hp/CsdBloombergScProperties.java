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
 * Title: CsdBloombergScProperties.java.
 * 
 * Author: CSD Development Team Date Created: 2026-01-06
 *
 * Purpose: Centralized configuration loader for Bloomberg integration. Loads properties from
 * bloomberg.properties file located in TAFJ_HOME/conf or falls back to classpath.
 *
 * Usage: CsdBloombergScProperties props = CsdBloombergScProperties.getInstance(); String host =
 * props.getMqHost();
 *
 * Modification Details:
 * 
 * ----06/01/25 - Initial version for centralized MQ configuration management
 * 
 * ----07/01/25 - 06/01/25 Added SC module configuration support with variable resolution
 */
public final class CsdBloombergScProperties {

    private static final Logger yLOGGER = Logger.getLogger(CsdBloombergScProperties.class.getName());
    private static final String PROPERTIES_FILE = "sc-bloomberg.properties";
    private static final String PLACEHOLDER_START = "${";
    private static final String PLACEHOLDER_END = "}";
    private static final String PLACEHOLDER_PAT = "${";
    private static CsdBloombergScProperties instance;
    private final Properties properties;

    /**
     * Private constructor - loads properties from file system or classpath.
     */
    private CsdBloombergScProperties() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Gets singleton instance of properties loader.
     */
    public static synchronized CsdBloombergScProperties getInstance() {
        if (instance == null) {
            instance = new CsdBloombergScProperties();
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
                yLOGGER.log(Level.INFO, "[CsdBloombergScProperties] Loaded from TAFJ_HOME: {0}", tafjPath);
            }
        }

        // Try Bloomberg UD directory
        if (!loaded) {
            Path udPath = Paths.get("D:", "Temenos", "R24", "bnk", "UD", "BLOOMBERG", "conf",
                    PROPERTIES_FILE);
            if (loadFromFile(udPath)) {
                loaded = true;
                yLOGGER.log(Level.INFO, "[CsdBloombergScProperties] Loaded from UD directory: {0}", udPath);
            }
        }

        // Try classpath
        if (!loaded && loadFromClasspath()) {
            loaded = true;
            yLOGGER.log(Level.INFO, "[CsdBloombergScProperties] Loaded from classpath: {0}", PROPERTIES_FILE);
        }

        if (!loaded) {
            yLOGGER.log(Level.WARNING, "[CsdBloombergScProperties] Could not load {0}, using defaults",
                    PROPERTIES_FILE);
            setDefaults();
        }
    }

    public void logLoadedProperties() {
        yLOGGER.log(Level.INFO, "[CsdBloombergScProperties] Loaded properties: {0}", properties);
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
                    String.format("[CsdBloombergScProperties] Error loading from file: %s", path), e);
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
            yLOGGER.log(Level.WARNING, "[CsdBloombergScProperties] Error loading from classpath", e);
        }
        return false;
    }

    /**
     * Sets default properties if file cannot be loaded.
     */

    private void setDefaults() {
        // Adapter Run Mode Defaults
        properties.setProperty("sc.ofs.adapter", "WMQ");
        properties.setProperty("sc.ofs.function", "INPUT");
        properties.setProperty("sc.ofs.source", "OFS.BMRG");
        properties.setProperty("sc.ofs.version.def", "SECURITY.MASTER,RAD");
        properties.setProperty("sc.ofs.version.cbn", "SECURITY.MASTER,CBN");

        // SC defaults - WMQ Mode
        properties.setProperty("sc.mq.host", "172.22.105.46");
        properties.setProperty("sc.mq.port", "1414");
        properties.setProperty("sc.mq.channel", "DEV.APP.SVRCONN");
        properties.setProperty("sc.mq.qmgr", "QM_BLOOMBERG");
        properties.setProperty("sc.mq.user", "");
        properties.setProperty("sc.mq.password", "");
        properties.setProperty("sc.mq.ack", "auto");
        properties.setProperty("sc.mq.inbound.queue", "SC.INBOUND.QUEUE");
        properties.setProperty("sc.mq.outbound.queue", "SC.OUTBOUND.QUEUE");

        // SC defaults - File Mode
        properties.setProperty("sc.fs.bloomberg.base.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG");
        properties.setProperty("sc.fs.inbound.dir", PLACEHOLDER_PAT + "sc.fs.bloomberg.base.dir}\\IN\\SC");
        properties.setProperty("sc.fs.outbound.dir", PLACEHOLDER_PAT + "sc.fs.bloomberg.base.dir}\\OUT\\SC");
        properties.setProperty("sc.fs.done.dir", PLACEHOLDER_PAT + "sc.fs.bloomberg.base.dir}\\DONE\\SC");
        properties.setProperty("sc.fs.error.dir", PLACEHOLDER_PAT + "sc.fs.bloomberg.base.dir}\\ERROR\\SC");
        properties.setProperty("sc.fs.file.pattern", "*.json");
    }

    // ====
    // ==== OFS Adapter PROPERTIES GETTERS ====
    // ====

    public String getAdapterMode() {
        return properties.getProperty("sc.ofs.adapter", "WMQ");
    }

    public String getOfsSource() {
        return properties.getProperty("sc.ofs.source", "OFS.BRMG");
    }

    public String getOfsFunction() {
        return properties.getProperty("sc.ofs.function", "INPUT");
    }

    public String getOfsVersionDef() {
        return properties.getProperty("sc.ofs.version.def", "SECURITY.MASTER,RAD");
    }

    public String getOfsVersion() {
        return properties.getProperty("sc.ofs.version.cbn", getOfsVersionDef());
    }

    // ====
    // ==== MQ Adapter PROPERTIES GETTERS ====
    // ====

    public String getMqHost() {
        return properties.getProperty("sc.mq.host", "172.22.105.46");
    }

    public int getMqPort() {
        return Integer.parseInt(properties.getProperty("sc.mq.port", "1414"));
    }

    public String getMqChannel() {
        return properties.getProperty("sc.mq.channel", "DEV.APP.SVRCONN");
    }

    public String getMqQueueManager() {
        return properties.getProperty("sc.mq.qmgr", "QM_BLOOMBERG");
    }

    public String getMqUser() {
        return properties.getProperty("sc.mq.user", "");
    }

    public String getMqPassword() {
        return properties.getProperty("sc.mq.password", "");
    }

    public String getMqAckMode() {
        return properties.getProperty("sc.mq.ack", "auto");
    }

    public String getMqInboundQueue() {
        return properties.getProperty("sc.mq.inbound.queue", "SC.INBOUND.QUEUE");
    }

    public String getMqOutboundQueue() {
        return properties.getProperty("sc.mq.outbound.queue", "SC.OUTBOUND.QUEUE");
    }

    // ====
    // ==== File Adapter PROPERTIES GETTERS ====
    // ====

    public String getFsInboundDir() {
        return resolveValue(
                properties.getProperty("sc.fs.inbound.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\IN\\SC"));
    }

    public String getFsOutboundDir() {
        return resolveValue(properties.getProperty("sc.fs.outbound.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\OUT\\SC"));
    }

    public String getFsDoneDir() {
        return resolveValue(
                properties.getProperty("sc.fs.done.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\DONE\\SC"));
    }

    public String getFsErrorDir() {
        return resolveValue(
                properties.getProperty("sc.fs.error.dir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\ERROR\\SC"));
    }

    public String getFsFilePattern() {
        return properties.getProperty("sc.fs.file.pattern", "*.json");
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