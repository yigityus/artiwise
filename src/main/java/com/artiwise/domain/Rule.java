package com.artiwise.domain;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class Rule {
    private String name;
    private String keywords;
    private List<Map<String, List<String>>> conditions;

    public List<String> getSplittedKeywords() {
        String[] split = keywords.split(",");
        return Arrays
                .stream(split)
                .map(s -> s.trim())
                .collect(Collectors.toList());
    }

}
