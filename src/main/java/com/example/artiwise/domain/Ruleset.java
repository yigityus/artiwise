package com.example.artiwise.domain;

import lombok.Data;

import java.util.List;

@Data
public class Ruleset {
    private String name;
    private List<Rule> rules;
}
