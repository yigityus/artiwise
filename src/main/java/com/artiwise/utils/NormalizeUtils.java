package com.artiwise.utils;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NormalizeUtils {
    static final List<String> stopWords = Arrays.asList("hiç", "ile", "ki", "doç", "dr");

    public static String normalize(String text) {
        if (StringUtils.hasText(text)) {
            text = text.toLowerCase(new Locale("tr")).replaceAll("[^a-zA-Z iğüöşçÇŞÖİĞÜı]", "");
            text = text.replaceAll(stopWords.stream().collect(Collectors.joining("|")), "");
            return text;
        }

        return null;
    }
}
