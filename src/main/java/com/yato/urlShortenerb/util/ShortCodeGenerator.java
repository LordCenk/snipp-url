package com.yato.urlShortenerb.util;

import lombok.NoArgsConstructor;

import java.security.SecureRandom;
@NoArgsConstructor
public final class ShortCodeGenerator {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final int DEFAULT_LENGTH=7;

    public static String generate(){
        StringBuilder sb= new StringBuilder(DEFAULT_LENGTH);
        for(int i=0;i<DEFAULT_LENGTH;i++){
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
