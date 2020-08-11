package com.artiwise.domain;

import com.artiwise.utils.NormalizeUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.artiwise.utils.NormalizeUtils.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class News {
    private Long id;
    private String url;
    private String name;
    private String lang;
    private String type;
    private List<String> tags;
    private List<String> categories;
    private String title;
    private String description;
    private String content;
    private Instant crawl_date;
    private Instant modified_date;
    private Instant published_date;

    private List<String> matchedRules = new ArrayList<>();

    public String getText() {
        return normalize(getTitle()) + " " +
                normalize(getDescription()) + " " +
                normalize(getContent());
    }

}
