package com.mcbc.tmb.tcsp;

/**
 * TODO: Document me!
 *
 * @author Temenos
 *
 */

import java.security.Provider;
import java.security.Security;
import javax.crypto.SecretKeyFactory;
 
public class ListProviders {
    public static void main(String[] args) {
        // Print out all registered providers
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            System.out.println("Provider: " + provider.getName());
            // Check if the provider supports PBKDF2WithHmacSHA1
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1", provider);
                System.out.println("\tSupports PBKDF2WithHmacSHA1");
            } catch (Exception e) {
                System.out.println("\tDoes not support PBKDF2WithHmacSHA1");
            }
        }
    }
}

