package com.mcbc.tmb.ws02;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Document me!
 *
 * @author debdas
 *
 */


    
public class StringSplitter {
        public static List<String> splitString(String input, int chunkSize) {
            List<String> result = new ArrayList<>();
            int length = input.length();
            
            for (int i = 0; i < length; i += chunkSize) {
                result.add(input.substring(i, Math.min(length, i + chunkSize)));
            }
            
            return result;
        }
}
