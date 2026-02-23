package com.mcbc.tmb.delivery;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.T24TypesConvertibleHelper;
import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.de.deliveryhook.Field;
import com.temenos.t24.api.hook.system.Delivery;
/**
 * DE.103.ALT - Modify handoff for alternate account
 * 
 * Equivalent jBC logic:
 *   Altaccount = R.HANDOFF(2)<102>
 *   trimAcct = Altaccount[6,99]
 *   R.HANDOFF(9)<1> = trimAcct
 *
 * @author shegs
 */
public class De103Alt extends Delivery {
    
    private static Logger logger = LoggerFactory.getLogger("API");
    

    @Override
    public List<Field> mapAdditionalDataToMessageType(
            TStructure record1, 
            TStructure record2, 
            TStructure record3,
            TStructure record4, 
            TStructure record5, 
            TStructure record6, 
            TStructure record7, 
            TStructure record8,
            String mappingKey, 
            TStructure record11) {
        
        String record2Values = T24TypesConvertibleHelper.toDynArray(record2);
        logger.debug("DeMapMt200------Incoming---record2---" + record2Values);
        
        String string2 = "12345";
        String[] words = record2Values.split("ï£¾");
        
        String altAccount = words[2];
        String trimAcct = "";
        if (altAccount != null && altAccount.length() >= 6) {
            trimAcct = altAccount.substring(5); // drop first 5 chars
        }

        words[77] = trimAcct;

        List<Field> finMapArray = new ArrayList<>();
        for (int i = 0; i <= words.length - 1; i++) {
            Field field = new Field();
            field.setValue(words[i]);

            finMapArray.add(field);
        }

        return finMapArray;
    }
    
}