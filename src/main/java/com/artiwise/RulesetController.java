package com.artiwise;

import com.artiwise.config.RulesetConfig;
import com.artiwise.domain.News;
import com.artiwise.domain.Rule;
import com.artiwise.domain.Ruleset;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class RulesetController {
    private final RestTemplate restTemplate;
    private final RulesetConfig rulesetConfig;
    private final ObjectMapper objectMapper;

    public RulesetController(RestTemplate restTemplate,
                             RulesetConfig rulesetConfig, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.rulesetConfig = rulesetConfig;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public ResponseEntity<String> rulesetsProcessor()
            throws IllegalAccessException, NoSuchFieldException, IOException {

        log.info("resultsetProcessor started");

        int counter = process();

        return ResponseEntity.of(Optional.of(String.format("Success! " + counter + " itmes are procssed")));
    }


    private int process() throws NoSuchFieldException, IllegalAccessException, IOException {
        int i = 0;
        int counter = 0;
        List<Ruleset> rulesets = rulesetConfig.getRulesets();

        log.info(rulesets.toString());

        List<News> body = fetchNews(restTemplate, i);

        while (body.size() > 0) {
            counter += body.size();
            List<News> filtered = getFilteredNews(rulesets, body);
            Map<String, List<News>> map = new HashMap<>();
            for (News news : filtered) {
                for (Ruleset ruleset : rulesets) {
                    List<Rule> rules = ruleset.getRules();

                    for (Rule rule : rules) {
                        processNews(news, rule);
                    }

                    if (!CollectionUtils.isEmpty(news.getMatchedRules())) {
                        map.computeIfAbsent(ruleset.getName(), k -> new ArrayList<>()).add(news);
                    }
                }
            }

            printMap(map);
            body = fetchNews(restTemplate, ++i);
        }
        return counter;
    }

    private void processNews(News news, Rule rule) throws NoSuchFieldException, IllegalAccessException {
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
                passed = isPassed(news, condition);
            }

            if (passed) {
                news.getMatchedRules().add(rule.getName());
            }
        }
    }

    private void printMap(Map<String, List<News>> map) throws IOException {
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
    }

    private boolean isPassed(News news, Map<String, List<String>> condition)
            throws NoSuchFieldException, IllegalAccessException {
        for (String key : condition.keySet()) {
            List<String> values = condition.get(key);
            Field field = news.getClass().getDeclaredField(key);
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
                return false;
            }
        }
        return true;
    }

    private List<News> getFilteredNews(List<Ruleset> rulesets, List<News> body) {
        List<News> filtered = new ArrayList<>();
        for (News news : body) {
            List<String> keywords = new ArrayList<>();
            for (Ruleset ruleset : rulesets) {
                keywords.addAll(ruleset.getKeywords());
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
        String artiwiseNewsUrl = "http://mock.artiwise.com/api/news?_page=" + i + "&_limit=100";
        ResponseEntity<List<News>> exchange = restTemplate.exchange(
                artiwiseNewsUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {
                });
        List<News> body = exchange.getBody();
        return body;
    }


}
