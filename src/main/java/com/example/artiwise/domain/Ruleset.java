package com.example.artiwise.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Ruleset {
    private String name;
    private List<Rule> rules;

    public List<String> getSplittedKeywords() {
        List<String> keywords = new ArrayList<>();
        for (Rule rule : rules) {
            keywords.addAll(rule.getSplittedKeywords());
        }
        return keywords;
    }

}
