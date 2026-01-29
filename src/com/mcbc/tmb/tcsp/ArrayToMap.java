package com.mcbc.tmb.tcsp;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Document me!
 *
 * @author debdas
 *
 */

    
    public class ArrayToMap {
     
        /**
     * @return the keys
     */
    public String[] getKeys() {
        return keys;
    }





    /**
     * @param keys the keys to set
     */
    public void setKeys(String[] keys) {
        this.keys = keys;
    }





    /**
     * @return the values
     */
    public String[] getValues() {
        return values;
    }





    /**
     * @param values the values to set
     */
    public void setValues(String[] values) {
        this.values = values;
    }





        String[] keys;
        String[] values;  
        
        
        
        
        
        public  void Convert(String[] args) {


            // Create a Map
            Map<String, String> map = new HashMap<>();

            // Populate the map with key-value pairs
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], values[i]);
            }

            // Print the map
            System.out.println(map);
        }
    } 


