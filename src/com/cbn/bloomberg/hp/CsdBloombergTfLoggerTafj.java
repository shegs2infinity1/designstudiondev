package com.cbn.bloomberg.hp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 * TafjLoggerHelper
 * 
 * This version preserves the existing flow and structure. Only enhancement: picks base log folder
 * and suffix from tmb-logger.properties.
 * 
 * Properties supported: logger.default.folder=/opt logger.path.suffix=/log_T24
 * logger.max.file.size=5000000 logger.max.file.count=10
 */
public final class CsdBloombergTfLoggerTafj {

    private CsdBloombergTfLoggerTafj() {
        // Utility class
    }

    public static Logger forClass(Class<?> owner) {
        final Logger log = Logger.getLogger(owner.getName());
        synchronized (log) {

            if (log.getHandlers().length > 0) {
                return log; // already configured
            }
            log.setUseParentHandlers(false);

            Properties logProps = new Properties();
            try (InputStream in = CsdBloombergTfLoggerTafj.class.getResourceAsStream("/fx-tafjlogging.properties")) {
                if (in != null) {
                    logProps.load(in);
                }
            } catch (IOException e) {
                // Fallback silently, preserve behavior
            }

            // Get config or use defaults
            String defaultFolder = logProps.getProperty("logger.default.folder", "/opt");
            String pathSuffix = logProps.getProperty("logger.path.suffix", "/log_T24");
            int maxFileSize = Integer.parseInt(logProps.getProperty("logger.max.file.size", "5000000"));
            int maxFileCount = Integer.parseInt(logProps.getProperty("logger.max.file.count", "10"));

            final String tafjHome = System.getenv("TAFJ_HOME");
            final String logsPath = ((tafjHome != null && !tafjHome.trim().isEmpty()) ? tafjHome : defaultFolder)
                    + pathSuffix;
            final String pattern = java.nio.file.Paths.get(logsPath,
                    owner.getSimpleName() + "_" + LocalDate.now() + "_%g.log").toString();
            // === END enhancement ===

            try {
                Files.createDirectories(java.nio.file.Paths.get(logsPath));
                FileHandler fh = new FileHandler(pattern, maxFileSize, maxFileCount, true);

                fh.setFormatter(new SimpleFormatter());
                fh.setLevel(Level.ALL);
                log.addHandler(fh);
                log.setLevel(Level.ALL);
            } catch (IOException | SecurityException e) {
                ConsoleHandler ch = new ConsoleHandler();
                ch.setFormatter(new SimpleFormatter());

                ch.setLevel(Level.ALL);
                log.addHandler(ch);
                log.setLevel(Level.ALL);
                log.log(Level.SEVERE, "Failed to initialize file logger; using console fallback", e);
            }
        }
        return log;
    }
}
