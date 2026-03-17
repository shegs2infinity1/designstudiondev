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
 * =============================================================================
 * CSD API Title: CbnTfProperties.java
 * Author: CSD Development Team
 * Created: 2025-10-27
 * Last Modified: 2026-03-15
 * =============================================================================
 *
 * PURPOSE: Centralized configuration loader for Bloomberg integration.
 * Loads properties from bloomberg.properties file located in TAFJ_HOME/conf
 * or falls back to classpath.
 *
 * MODIFICATION HISTORY:
 * - 2025-10-27 | Initial version for centralized MQ configuration management
 * - 2025-11-07 | Added TF module configuration support with variable resolution
 * - 2026-01-11 | Added new operation to process TF Deals Local App
 * - 2026-02-03 | Added ST (SEC_TRADE) OFS version configuration
 * - 2026-03-15 | Refactored SC/ST versions for EB.CBN.INSTRUMENT.DETAIL.INT & EB.CBN.BOND.TRADE.INT
 * =============================================================================
 */
public final class CbnTfProperties {

    private static final Logger yLogger = Logger.getLogger(CbnTfProperties.class.getName());
    private static final String PROPERTIES_FILE = "CbnTfProperties.properties";
    private static final String PLACEHOLDER_START = "${";
    private static final String PLACEHOLDER_CLOSE = "}";
    private static CbnTfProperties instance;
    private final Properties properties;

    private CbnTfProperties() {
        this.properties = new Properties();
        loadProperties();
    }

    public static synchronized CbnTfProperties getInstance() {
        if (instance == null) {
            instance = new CbnTfProperties();
        }
        return instance;
    }

    private void loadProperties() {
        boolean loaded = false;
        String tafj = System.getenv("TAFJ_HOME");
        if (tafj != null && !tafj.isEmpty()) {
            Path tafjPath = Paths.get(tafj, "conf", PROPERTIES_FILE);
            if (loadFromFile(tafjPath)) {
                loaded = true;
                yLogger.log(Level.INFO, "[CbnTfProperties] Loaded from TAFJ_HOME: {0}", tafjPath);
            }
        }
        if (!loaded) {
            Path udPath = Paths.get("D:", "Temenos", "R24", "bnk", "UD", "BLOOMBERG", "conf", PROPERTIES_FILE);
            if (loadFromFile(udPath)) {
                loaded = true;
                yLogger.log(Level.INFO, "[CbnTfProperties] Loaded from UD directory: {0}", udPath);
            }
        }
        if (!loaded && loadFromClasspath()) {
            loaded = true;
            yLogger.log(Level.INFO, "[CbnTfProperties] Loaded from classpath: {0}", PROPERTIES_FILE);
        }
        if (!loaded) {
            yLogger.log(Level.WARNING, "[CbnTfProperties] Could not load {0}, using defaults", PROPERTIES_FILE);
            setDefaults();
        }
    }

    public void logLoadedProperties() {
        yLogger.log(Level.INFO, "[CbnTfProperties] Loaded properties: {0}", properties);
    }

    private boolean loadFromFile(Path path) {
        if (!Files.exists(path)) return false;
        try (InputStream is = Files.newInputStream(path)) {
            properties.load(is);
            return true;
        } catch (IOException e) {
            yLogger.log(Level.WARNING, String.format("[CbnTfProperties] Error loading from file: %s", path), e);
            return false;
        }
    }

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
        properties.setProperty("tf.nfs.inbound.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\IN\\TF");
        properties.setProperty("tf.nfs.outbound.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\OUT\\TF");
        properties.setProperty("tf.nfs.done.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\DONE\\TF");
        properties.setProperty("tf.nfs.error.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\ERROR\\TF");

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

        // 4. PD Version Defaults
        properties.setProperty("tf.ofs.version.pddf", "EB.CBN.PLACEMENT.DEPOSIT,RAD");
        properties.setProperty("tf.ofs.version.pdep", "EB.CBN.PLACEMENT.DEPOSIT,CBN");

        // 5. SC / ST Version Defaults (refactored 2026 – Bloomberg SC flow)
        // New clean keys
        properties.setProperty("tf.ofs.version.sc.master",       "EB.CBN.INSTRUMENT.DETAIL.INT,BOND");
        properties.setProperty("tf.ofs.version.sc.ndic.trade",   "EB.CBN.BOND.TRADE.INT,NDIC.TRADDE");
        properties.setProperty("tf.ofs.version.sc.cbn.trade",    "EB.CBN.BOND.TRADE.INT,CBN.TRADDE");

        // Backward compatibility during transition
        properties.setProperty("tf.ofs.version.scdf", "EB.CBN.INSTRUMENT.DETAIL.INT,BOND");
        properties.setProperty("tf.ofs.version.sctd", "EB.CBN.INSTRUMENT.DETAIL.INT,BOND");
        properties.setProperty("tf.ofs.version.stdf", "EB.CBN.BOND.TRADE.INT,RAD");
        properties.setProperty("tf.ofs.version.sttd", "EB.CBN.BOND.TRADE.INT,CBN");
    }

    // ========================================================================
    // ==== Default Adapter & MQ Getters (unchanged) ====
    // ========================================================================
    public String getDefAdapter() { return properties.getProperty("tf.def.adapter", "WMQ"); }

    public String getWmqHost() { return properties.getProperty("tf.wmq.host", "172.22.105.46"); }
    public int getWmqPort() { return Integer.parseInt(properties.getProperty("tf.wmq.port", "1414")); }
    public String getWmqChannel() { return properties.getProperty("tf.wmq.channel", "DEV.APP.SVRCONN"); }
    public String getWmqQueueManager() { return properties.getProperty("tf.wmq.manager", "QM_BLOOMBERG"); }
    public String getWmqUser() { return properties.getProperty("tf.wmq.username", ""); }
    public String getWmqPassword() { return properties.getProperty("tf.wmq.password", ""); }
    public String getWmqAckMode() { return properties.getProperty("tf.wmq.ackledge", "auto"); }
    public String getWmqInboundQueue() { return properties.getProperty("tf.wmq.inbound.queue", "TF.INBOUND.QUEUE"); }
    public String getWmqOutboundQueue() { return properties.getProperty("tf.wmq.outbound.queue", "TF.OUTBOUND.QUEUE"); }

    // ========================================================================
    // ==== File Node Getters (unchanged) ====
    // ========================================================================
    public String getNfsFilePattern() { return properties.getProperty("tf.nfs.pattern", "*.json"); }
    public String getNfsInboundDir() { return resolveValue(properties.getProperty("tf.nfs.inbound.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\IN\\TF")); }
    public String getNfsOutboundDir() { return resolveValue(properties.getProperty("tf.nfs.outbound.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\OUT\\TF")); }
    public String getNfsDoneDir() { return resolveValue(properties.getProperty("tf.nfs.done.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\DONE\\TF")); }
    public String getNfsErrorDir() { return resolveValue(properties.getProperty("tf.nfs.error.dir", PLACEHOLDER_START + "tf.nfs.basedir" + PLACEHOLDER_CLOSE + "\\ERROR\\TF")); }

    // ========================================================================
    // ==== OFS General Getters (unchanged) ====
    // ========================================================================
    public String getOfsSource() { return properties.getProperty("tf.ofs.source", "OFS.BMRG"); }
    public String getOfsFunction() { return properties.getProperty("tf.ofs.function", "INPUT"); }

    // ========================================================================
    // ==== Module-specific OFS Version Getters ====
    // ========================================================================

    // FT
    public String getOfsVersionFtDef() { return properties.getProperty("tf.ofs.version.ftdf", "FUNDS.TRANSFER,AC"); }
    public String getOfsVersionFt() { return properties.getProperty("tf.ofs.version.ftsd", getOfsVersionFtDef()); }

    // FX
    public String getOfsVersionFxDef() { return properties.getProperty("tf.ofs.version.fxdf", "FOREX,FX.RAD"); }
    public String getOfsVersionFx(String dealType) {
        if (dealType == null || dealType.trim().isEmpty()) return getOfsVersionFxDef();
        String key = "tf.ofs.version.fx" + dealType.trim().toLowerCase();
        yLogger.log(Level.INFO, "[CbnTfProperties] FX Transaction Version: {0}", key);
        return properties.getProperty(key, getOfsVersionFxDef());
    }

    // PR
    public String getOfsVersionPrDef() { return properties.getProperty("tf.ofs.version.prdf", "EB.CBN.REPO.PLACEMENT,RAD"); }
    public String getOfsVersionPr() { return properties.getProperty("tf.ofs.version.prep", getOfsVersionPrDef()); }

    // PD
    public String getOfsVersionPdDef() { return properties.getProperty("tf.ofs.version.pddf", "EB.CBN.PLACEMENT.DEPOSIT,RAD"); }
    public String getOfsVersionPd() { return properties.getProperty("tf.ofs.version.pdep", getOfsVersionPdDef()); }
    public String getOfsVersionPdFgn() {
        return properties.getProperty("tf.ofs.version.pdfng", getOfsVersionPdDef());
    }
    // === SC / ST (Security related – refactored 2026) ===

    // Deprecated / transition methods – log warning when used
    public String getOfsVersionScDef() {
        yLogger.warning("[CbnTfProperties] Deprecated: getOfsVersionScDef() → use getOfsVersionScMaster()");
        return properties.getProperty("tf.ofs.version.scdf", "EB.CBN.INSTRUMENT.DETAIL.INT,BOND");
    }

    public String getOfsVersionSc() {
        yLogger.warning("[CbnTfProperties] Deprecated: getOfsVersionSc() → use getOfsVersionScMaster()");
        return properties.getProperty("tf.ofs.version.sctd", "EB.CBN.INSTRUMENT.DETAIL.INT,BOND");
    }

    public String getOfsVersionStDef() {
        yLogger.warning("[CbnTfProperties] Deprecated: getOfsVersionStDef() → use getOfsVersionScNdicTrade() or getOfsVersionScCbnTrade()");
        return properties.getProperty("tf.ofs.version.stdf", "EB.CBN.BOND.TRADE.INT,RAD");
    }

    public String getOfsVersionSt() {
        yLogger.warning("[CbnTfProperties] Deprecated: getOfsVersionSt() → use getOfsVersionScNdicTrade() or getOfsVersionScCbnTrade()");
        return properties.getProperty("tf.ofs.version.sttd", "EB.CBN.BOND.TRADE.INT,CBN");
    }

    // New clean getters – recommended for all new code
    public String getOfsVersionScMaster() {
        return properties.getProperty("tf.ofs.version.sc.master", "EB.CBN.INSTRUMENT.DETAIL.INT,BOND");
    }

    public String getOfsVersionScNdicTrade() {
        return properties.getProperty("tf.ofs.version.sc.ndic.trade", "EB.CBN.BOND.TRADE.INT,NDIC.TRADDE");
    }

    public String getOfsVersionScCbnTrade() {
        return properties.getProperty("tf.ofs.version.sc.cbn.trade", "EB.CBN.BOND.TRADE.INT,CBN.TRADDE");
    }

    // ========================================================================
    // ==== UTILITY METHODS ====
    // ========================================================================

    private String resolveValue(String value) {
        if (value == null || !value.contains(PLACEHOLDER_START)) return value;
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

    public Properties getProperties() {
        return (Properties) properties.clone();
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}