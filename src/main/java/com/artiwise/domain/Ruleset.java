package com.artiwise.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Ruleset {
    private String name;
    private List<Rule> rules;

    public List<String> getKeywords() {
        List<String> keywords = new ArrayList<>();
        for (Rule rule : rules) {
            keywords.addAll(rule.getSplittedKeywords());
        }
        return keywords;
    }

}
