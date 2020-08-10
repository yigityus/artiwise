package com.example.artiwise;

import com.example.artiwise.domain.News;
import com.example.artiwise.domain.Rule;
import com.example.artiwise.domain.Ruleset;
import com.example.artiwise.utils.RulesetProperties;
import com.example.artiwise.utils.ServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Token;
import org.ahocorasick.trie.Trie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootApplication
public class ArtiwiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtiwiseApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }


    @Autowired
    private RulesetProperties rulesetProperties;


    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) {
        return args -> {
            final String artiwiseNewsUrl = "http://mock.artiwise.com/api/news?_page=0&_limit=1";
            Field[] allFields = News.class.getDeclaredFields();
            for (Field field : allFields) {
                log.info(field.getName());
            }

            ResponseEntity<List<News>> exchange = restTemplate.exchange(
                    artiwiseNewsUrl, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });

            List<Ruleset> rulesets = rulesetProperties.getRulesets();
            List<News> body = exchange.getBody();
            List<News> filtered = new ArrayList<>();

            for (News news : body) {
                String keywords = rulesets.stream()
                        .map(Ruleset::getRules)
                        .map(rules -> rules.stream()
                                .map(Rule::getKeywords)
                                .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(","));

                log.info(keywords);
                String[] splitted = keywords.split(",");

                Trie trie = Trie.builder()
                        .addKeywords(splitted)
                        .build();

                if (trie.parseText(news.getText()).size() > 0) {
                    filtered.add(news);
                }
            }


            for (News news : filtered) {
                for (Ruleset ruleset : rulesets) {
                    log.info(news.getText());
                    List<Rule> rules = ruleset.getRules();

                    for (Rule rule : rules) {
                        String[] splittedKeywors = rule.getKeywords().split(",");

                        Trie trie = Trie.builder()
                                .addKeywords(splittedKeywors)
                                .build();

                        Collection<Emit> emits = trie.parseText(news.getText());
                        List<String> hits = emits.stream().map(Emit::getKeyword).distinct().collect(Collectors.toList());

                        if (hits.containsAll(Arrays.asList(splittedKeywors))) {

                            List<Map<String, List<String>>> conditions = rule.getConditions();
                            for (Map<String, List<String>> condition : conditions) {
                                for (String key : condition.keySet()) {
                                    List<String> values = condition.get(key);
                                    Field field = news.getClass().getDeclaredField(key);
                                    log.info(String.valueOf(field.getType()));
                                    List<String> items;

                                    if (field.getType().equals(List.class)) {
                                        field.setAccessible(true);
                                        items = (List<String>) field.get(news);

                                    } else {
                                        field.setAccessible(true);
                                        String item = (String) field.get(news);
                                        items = Collections.singletonList(item);
                                    }

                                    if (!Collections.disjoint(items, values)) {
                                        break;
                                    }

                                    log.info(field.getName());
                                }
                            }


                            news.getMatchedRules().add(rule);
                        }



                    }

                }
            }


            log.info(rulesetProperties.toString());
        };
    }


}
