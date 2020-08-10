package com.example.artiwise.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Rule {
    private String name;
    private Condition condition;
    private String keywords;
    private List<Map<String, List<String>>> conditions;



}
