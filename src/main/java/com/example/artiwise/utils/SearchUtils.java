package com.example.artiwise.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SearchUtils {
    List<String> stopWords = Arrays.asList("hiç", "ile", "ki", "doç", "dr");

    public static String normalize(String text) {
        text = text.toLowerCase(new Locale("tr")).replaceAll("[^a-zA-Z iğüöşçÇŞÖİĞÜı]", "");
        return text;
    }
}
