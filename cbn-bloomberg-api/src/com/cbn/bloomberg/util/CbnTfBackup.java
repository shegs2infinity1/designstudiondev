package com.cbn.bloomberg.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CbnTfBackup {
    private static final Logger LOGGER = Logger.getLogger(CbnTfBackup.class.getName());
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public static void backupMessage(String messageBody, String module, String originalId) {
        try {
            CbnTfProperties props = CbnTfProperties.getInstance();
            String backupDirStr = props.getProperty("tf.nfs.backup.dir", "/t24app/app/bnk/UD/BLOOMBERG/BACKUP");
            Path backupPath = Paths.get(backupDirStr, module);
            
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            String timestamp = LocalDateTime.now().format(TS_FMT);
            String fileName = String.format("%s_%s.json", module, timestamp);
            Files.write(backupPath.resolve(fileName), messageBody.getBytes());
            
            LOGGER.log(Level.INFO, "[CbnTfBackup] Backed up {0} message to {1}", new Object[]{module, fileName});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CbnTfBackup] Failed to backup message", e);
        }
    }
}