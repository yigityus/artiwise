package com.artiwise.config;

import com.artiwise.domain.Ruleset;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "artiwise")
public class RulesetConfig {
    private List<Ruleset> rulesets = new ArrayList<>();
}
