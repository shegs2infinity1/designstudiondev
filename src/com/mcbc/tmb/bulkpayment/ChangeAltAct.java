package com.mcbc.tmb.bulkpayment;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.*;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.de.deliveryhook.Field;
import com.temenos.t24.api.hook.system.Delivery;
import com.temenos.t24.api.records.deoheader.DeOHeaderRecord;


public class ChangeAltAct extends Delivery {

    private static final Logger logger = Logger.getLogger(ChangeAltAct.class.getName());

    static {
        try {
            String logFilePath = "/u01/t24appl/r23uat2/tafj/log/DeDebug.log";
            FileHandler fileHandler = new FileHandler(logFilePath, true); // append = true
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false); // prevent logging to console
        } catch (IOException | IllegalStateException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to initialize logger", e);
        }
    }

    @Override
    public List<Field> mapAdditionalDataToMessageType(
            TStructure record1, TStructure record2, TStructure record3, TStructure record4,
            TStructure record5, TStructure record6, TStructure record7, TStructure record8,
            String mappingKey, TStructure record11) {

        logger.info("=== Entering mapAdditionalDataToMessageType ===");
        logger.info("Mapping Key: " + mappingKey);
        
       DeOHeaderRecord derec = new DeOHeaderRecord(record1);
       derec.get

        dumpTStructure("Record1", record1);
        dumpTStructure("Record2", record2);
        dumpTStructure("Record3", record3);
        dumpTStructure("Record4", record4);
        dumpTStructure("Record5", record5);
        dumpTStructure("Record6", record6);
        dumpTStructure("Record7", record7);
        dumpTStructure("Record8", record8);
        dumpTStructure("Record11", record11);

        // You can now build your List<Field> here based on what you find in the logs
        return new ArrayList<>();
    }

    private void dumpTStructure(String label, TStructure record) {
        logger.info("----- " + label + " -----");
        try {
            Set<String> fieldNames = record.getFieldNames();
            for (String field : fieldNames) {
                String value = record.getFieldValue(field);
                logger.info(field + " = " + value);
            }
        } catch (Exception e) {
            logger.warning("Error reading " + label + ": " + e.getMessage());
        }
    }
}
