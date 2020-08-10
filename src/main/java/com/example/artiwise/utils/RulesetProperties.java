package com.example.artiwise.utils;

import com.example.artiwise.domain.Ruleset;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Data
@ConfigurationProperties(prefix = "artiwise")
public class RulesetProperties {
    private List<Ruleset> rulesets = new ArrayList<>();
}
