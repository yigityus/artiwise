package com.example.artiwise;

import com.example.artiwise.domain.News;
import com.example.artiwise.domain.Rule;
import com.example.artiwise.domain.Ruleset;
import com.example.artiwise.utils.RulesetProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootApplication
public class ArtiwiseApplication {

    private final RulesetProperties rulesetProperties;
    private final ObjectMapper objectMapper;

    public ArtiwiseApplication(RulesetProperties rulesetProperties, ObjectMapper objectMapper) {
        this.rulesetProperties = rulesetProperties;
        this.objectMapper = objectMapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(ArtiwiseApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) {
        return args -> {
            int i = 0;
            List<Ruleset> rulesets = rulesetProperties.getRulesets();
            List<News> body = fetchNews(restTemplate, i);

            while (body.size() > 0) {
                List<News> filtered = getFilteredNews(rulesets, body);


                Map<String, List<News>> map = new HashMap<>();
                for (News news : filtered) {

                    for (Ruleset ruleset : rulesets) {
                        log.info(news.getText());
                        List<Rule> rules = ruleset.getRules();

                        for (Rule rule : rules) {

                            Trie trie = Trie.builder()
                                    .addKeywords(rule.getSplittedKeywords())
                                    .build();

                            Collection<Emit> emits = trie.parseText(news.getText());
                            List<String> hits = emits.stream()
                                    .map(Emit::getKeyword)
                                    .distinct()
                                    .collect(Collectors.toList());

                            if (hits.containsAll(rule.getSplittedKeywords())) {

                                List<Map<String, List<String>>> conditions = rule.getConditions();
                                boolean passed = true;
                                for (Map<String, List<String>> condition : conditions) {
                                    for (String key : condition.keySet()) {
                                        List<String> values = condition.get(key);
                                        Field field = news.getClass().getDeclaredField(key);
                                        log.info(String.valueOf(field.getType()));
                                        List<String> items = new ArrayList<>();

                                        if (field.getType().equals(List.class)) {
                                            field.setAccessible(true);
                                            List<String> list = (List<String>) field.get(news);
                                            items.addAll(list);
                                        } else {
                                            field.setAccessible(true);
                                            String item = (String) field.get(news);
                                            items = Collections.singletonList(item);
                                        }

                                        if (!CollectionUtils.containsAny(values, items)) {
                                            passed = false;
                                            break;
                                        }
                                    }
                                }

                                if (passed) {
                                    news.getMatchedRules().add(rule.getName());
                                }
                            }
                        }

                        if (!CollectionUtils.isEmpty(news.getMatchedRules())) {
                            map.computeIfAbsent(ruleset.getName(), k -> new ArrayList<>()).add(news);
                        }
                    }
                }

                for (String ruleset : map.keySet()) {

                    List<News> news = map.get(ruleset);
                    Path file = Paths.get(ruleset + ".txt");
                    if (!Files.exists(file)) {
                        Files.createFile(file);
                    }

                    Files.write(file,
                            objectMapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsBytes(news),
                            StandardOpenOption.APPEND);

                }

                body = fetchNews(restTemplate, ++i);
            }

        };
    }

    private List<News> getFilteredNews(List<Ruleset> rulesets, List<News> body) {
        List<News> filtered = new ArrayList<>();
        for (News news : body) {
            List<String> keywords = new ArrayList<>();
            for (Ruleset ruleset : rulesets) {
                keywords.addAll(ruleset.getSplittedKeywords());
            }
            Trie trie = Trie.builder()
                    .addKeywords(keywords)
                    .build();
            if (!CollectionUtils.isEmpty(trie.parseText(news.getText()))) {
                filtered.add(news);
            }
        }
        return filtered;
    }

    private List<News> fetchNews(RestTemplate restTemplate, int i) {
        String artiwiseNewsUrl = "http://mock.artiwise.com/api/news?_page="+ i +"&_limit=100";
        ResponseEntity<List<News>> exchange = restTemplate.exchange(
                artiwiseNewsUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {
                });
        List<News> body = exchange.getBody();
        return body;
    }


}
