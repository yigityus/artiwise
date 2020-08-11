package com.artiwise.domain;

import lombok.Data;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AllMatchCondition implements Condition {

    private List<String> keywords;
    private Rule rule;

    public AllMatchCondition(Rule rule, List<String> keywords) {
        this.rule = rule;
        this.keywords = keywords;
    }

    @Override
    public boolean test(News news) {
        Trie trie = Trie.builder()
                .addKeywords(rule.getSplittedKeywords())
                .build();
        Collection<Emit> emits = trie.parseText(news.getText());
        List<String> hits = emits.stream()
                .map(Emit::getKeyword)
                .distinct()
                .collect(Collectors.toList());
        return hits.containsAll(rule.getSplittedKeywords());
    }
}
