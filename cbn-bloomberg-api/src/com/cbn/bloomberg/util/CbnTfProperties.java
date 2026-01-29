package com.cbn.bloomberg.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Title: CbnTfProperties.java
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
 *         String host = props.getWmqHost();
 * 
 * @modification Details:
 * 
 *               27/10/25 - Initial version for centralized MQ configuration management
 * 
 *               07/11/25 - Added TF module configuration support with variable resolution
 * 
 *               11/01/26 - Added new operation to process TF Deals Local App
 */
public final class CbnTfProperties {

    private static final Logger yLogger = Logger.getLogger(CbnTfProperties.class.getName());
    private static final String PROPERTIES_FILE = "CbnTfProperties.properties";
    private static final String PLACEHOLDER_START = "${";
    private static final String PLACEHOLDER_CLOSE = "}";
    private static final String PLACEHOLDER_PATTN = "${";

    private static CbnTfProperties instance;
    private final Properties properties;

    /**
     * Private constructor - loads properties from file system or classpath.
     */
    private CbnTfProperties() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Gets singleton instance of properties loader.
     */
    public static synchronized CbnTfProperties getInstance() {
        if (instance == null) {
            instance = new CbnTfProperties();
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
                yLogger.log(Level.INFO, "[CbnTfProperties] Loaded from TAFJ_HOME: {0}", tafjPath);
            }
        }

        // Try Bloomberg UD directory
        if (!loaded) {
            Path udPath = Paths.get("D:", "Temenos", "R24", "bnk", "UD", "BLOOMBERG", "conf",
                    PROPERTIES_FILE);
            if (loadFromFile(udPath)) {
                loaded = true;
                yLogger.log(Level.INFO, "[CbnTfProperties] Loaded from UD directory: {0}", udPath);
            }
        }

        // Try classpath
        if (!loaded && loadFromClasspath()) {
            loaded = true;
            yLogger.log(Level.INFO, "[CbnTfProperties] Loaded from classpath: {0}",
                    PROPERTIES_FILE);
        }

        if (!loaded) {
            yLogger.log(Level.WARNING, "[CbnTfProperties] Could not load {0}, using defaults",
                    PROPERTIES_FILE);
            setDefaults();
        }
    }

    public void logLoadedProperties() {
        yLogger.log(Level.INFO, "[CbnTfProperties] Loaded properties: {0}", properties);
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
                    String.format("[CbnTfProperties] Error loading from file: %s", path), e);
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
            yLogger.log(Level.WARNING, "[CbnTfProperties] Error loading from classpath", e);
        }
        return false;
    }

    /**
     * Sets default properties if file cannot be loaded.
     */

    private void setDefaults() {
        // TF Adapter Node Constant Defaults
        properties.setProperty("tf.def.adapter", "WMQ");
        // TF WMQ Node defaults
        properties.setProperty("tf.wmq.host", "172.22.105.46");
        properties.setProperty("tf.wmq.port", "1414");
        properties.setProperty("tf.wmq.channel", "DEV.APP.SVRCONN");
        properties.setProperty("tf.wmq.manager", "QM_BLOOMBERG");
        properties.setProperty("tf.wmq.username", "");
        properties.setProperty("tf.wmq.password", "");
        properties.setProperty("tf.wmq.ackledge", "auto");
        properties.setProperty("tf.wmq.inbound.queue", "TF.INBOUND.QUEUE");
        properties.setProperty("tf.wmq.outbound.queue", "TF.OUTBOUND.QUEUE");
        // TF File Node defaults
        properties.setProperty("tf.nfs.pattern", "*.json");
        properties.setProperty("tf.nfs.basedir", "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG");
        properties.setProperty("tf.nfs.inbound.dir", PLACEHOLDER_PATTN + "tf.nfs.basedir}\\IN\\TF");
        properties.setProperty("tf.nfs.outbound.dir",
                PLACEHOLDER_PATTN + "tf.nfs.basedir}\\OUT\\TF");
        properties.setProperty("tf.nfs.done.dir", PLACEHOLDER_PATTN + "tf.nfs.basedir}\\DONE\\TF");
        properties.setProperty("tf.nfs.error.dir",
                PLACEHOLDER_PATTN + "tf.nfs.basedir}\\ERROR\\TF");

        // TF Adapter OFS Constant Defaults
        properties.setProperty("tf.ofs.source", "OFS.BMRG");
        properties.setProperty("tf.ofs.function", "INPUT");
        // 1. FT Version Defaults
        properties.setProperty("tf.ofs.version.ftdf", "FUNDS.TRANSFER,AC");
        properties.setProperty("tf.ofs.version.ftsd", "FUNDS.TRANSFER,CBN.BKSD.FMD");
        // 2. FX Version Defaults
        properties.setProperty("tf.ofs.version.fxdf", "FOREX,FX.RAD");
        properties.setProperty("tf.ofs.version.fxst", "FOREX,SPOTDEAL");
        properties.setProperty("tf.ofs.version.fxsp", "FOREX,FX.SWAP");
        properties.setProperty("tf.ofs.version.fxfw", "FOREX,FORWARDDEAL");
        properties.setProperty("tf.ofs.version.fxdt", "FX.CBN.BKSD.REQUEST,RAD");
        properties.setProperty("tf.ofs.version.fxsd", "FX.CBN.BKSD.REQUEST,BKSD.REQ");
        // 3. PR Version Defaults
        properties.setProperty("tf.ofs.version.prdf", "EB.CBN.REPO.PLACEMENT,RAD");
        properties.setProperty("tf.ofs.version.prep", "EB.CBN.REPO.PLACEMENT,CBN.REPO");
        // 4. PR Version Defaults
        properties.setProperty("tf.ofs.version.pddf", "EB.CBN.PLACEMENT.DEPOSIT,RAD");
        properties.setProperty("tf.ofs.version.pdep", "EB.CBN.PLACEMENT.DEPOSIT,CBN");
        // 5. SC Version Defaults
        properties.setProperty("tf.ofs.version.scdf", "SECURITY.MASTER,RAD");
        properties.setProperty("tf.ofs.version.sctd", "SECURITY.MASTER,CBN");

    }

    // ====
    // ==== Default Adapter Node PROPERTIES GETTERS ====\
    // ====

    public String getDefAdapter() {
        return properties.getProperty("tf.def.adapter", "WMQ");
    }

    // ====
    // ==== MQ PROPERTIES GETTERS ====\
    // ====

    public String getWmqHost() {
        return properties.getProperty("tf.wmq.host", "172.22.105.46");
    }

    public int getWmqPort() {
        return Integer.parseInt(properties.getProperty("tf.wmq.port", "1414"));
    }

    public String getWmqChannel() {
        return properties.getProperty("tf.wmq.channel", "DEV.APP.SVRCONN");
    }

    public String getWmqQueueManager() {
        return properties.getProperty("tf.wmq.manager", "QM_BLOOMBERG");
    }

    public String getWmqUser() {
        return properties.getProperty("tf.wmq.username", "");
    }

    public String getWmqPassword() {
        return properties.getProperty("tf.wmq.password", "");
    }

    public String getWmqAckMode() {
        return properties.getProperty("tf.wmq.ackledge", "auto");
    }

    public String getWmqInboundQueue() {
        return properties.getProperty("tf.wmq.inbound.queue", "TF.INBOUND.QUEUE");
    }

    public String getWmqOutboundQueue() {
        return properties.getProperty("tf.wmq.outbound.queue", "TF.OUTBOUND.QUEUE");
    }

    // ====
    // ==== TF MODULE PROPERTIES GETTERS ====
    // ====

    public String getNfsFilePattern() {
        return properties.getProperty("tf.nfs.pattern", "*.json");
    }

    public String getNfsInboundDir() {
        return resolveValue(properties.getProperty("tf.nfs.inbound.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\IN\\TF"));
    }

    public String getNfsOutboundDir() {
        return resolveValue(properties.getProperty("tf.nfs.inbound.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\OUT\\TF"));
    }

    public String getNfsDoneDir() {
        return resolveValue(properties.getProperty("tf.nfs.done.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\DONE\\TF"));
    }

    public String getNfsErrorDir() {
        return resolveValue(properties.getProperty("tf.nfs.error.dir",
                "D:\\Temenos\\R24\\bnk\\UD\\BLOOMBERG\\ERROR\\TF"));
    }

    // ====
    // ==== OFS Adapter PROPERTIES GETTERS ====\
    // ====

    public String getOfsSource() {
        return properties.getProperty("tf.ofs.source", "OFS.LOAD");
    }

    public String getOfsFunction() {
        return properties.getProperty("tf.ofs.function", "INPUT");
    }

    // 1. TF-FT OFS Versions
    public String getOfsVersionFtDef() {
        return properties.getProperty("tf.ofs.version.ftdf", "FUNDS.TRANSFER,AC");
    }

    public String getOfsVersionFt() {
        return properties.getProperty("tf.ofs.version.ftsd", getOfsVersionFtDef());
    }

    // 2. TF-FX OFS Versions
    public String getOfsVersionFxDef() {
        return properties.getProperty("tf.ofs.version.fxdf", "FOREX,RAD");
    }

    public String getOfsVersionFx(String dealType) {
        if (dealType == null || dealType.trim().isEmpty()) {
            return getOfsVersionFxDef();
        }
        String key = "tf.ofs.version.fx" + dealType.trim().toLowerCase();
        yLogger.log(Level.INFO, "[CbnTfProperties] FX Transaction Version: {0}", key);
        return properties.getProperty(key, getOfsVersionFxDef());
    }

    // 3. TF-PR OFS Versions
    public String getOfsVersionPrDef() {
        return properties.getProperty("tf.ofs.version.prdf", "EB.CBN.REPO.PLACEMENT,RAD");
    }

    public String getOfsVersionPr() {
        return properties.getProperty("tf.ofs.version.prep", getOfsVersionPrDef());
    }

    // 4. TF-PD OFS Versions
    public String getOfsVersionPdDef() {
        return properties.getProperty("tf.ofs.version.pddf", "EB.CBN.PLACEMENT.DEPOSIT,RAD");
    }

    public String getOfsVersionPd() {
        return properties.getProperty("tf.ofs.version.pdep", getOfsVersionPdDef());
    }

    // 5. TF-SC OFS Versions
    public String getOfsVersionScDef() {
        return properties.getProperty("tf.ofs.version.scdf", "SECURITY.MASTER,RAD");
    }

    public String getOfsVersionSc() {
        return properties.getProperty("tf.ofs.version.sctd", getOfsVersionScDef());
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