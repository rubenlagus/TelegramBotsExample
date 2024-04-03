package org.telegram.services;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Rae service
 */
@Slf4j
public class RaeService {
    private static final String BASEURL = "http://dle.rae.es/srv/"; ///< Base url for REST
    private static final String SEARCHEXACTURL = "search?m=30&w=";
    private static final String SEARCHWORDURL = "search?m=form&w=";
    private static final String WORDLINKBYID = "http://dle.rae.es/?id=";

    private final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();

    public List<RaeResult> getResults(String query) {
        List<RaeResult> results = new ArrayList<>();

        String completeURL;
        try {
            completeURL = BASEURL + SEARCHEXACTURL + URLEncoder.encode(query, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .header("content-type", "application/json")
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            Document document = Jsoup.parse(body.string());
                            Element article = document.getElementsByTag("article").first();
                            String articleId = null;
                            if (article != null) {
                                articleId = article.attributes().get("id");
                            }
                            Elements elements = document.select(".j");

                            if (elements.isEmpty()) {
                                results = getResultsWordSearch(query);
                            } else {
                                results = getResultsFromExactMatch(elements, query, articleId);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error getting RAE results", e);
        }

        return results;
    }

    private List<RaeResult> getResultsWordSearch(String query) {
        List<RaeResult> results = new ArrayList<>();

        String completeURL;
        try {
            completeURL = BASEURL + SEARCHWORDURL + URLEncoder.encode(query, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            Document document = Jsoup.parse(body.string());
                            Element list = document.select("body div ul").first();

                            if (list != null) {
                                Elements links = list.getElementsByTag("a");
                                if (!links.isEmpty()) {
                                    for (Element link : links) {
                                        List<RaeResult> partialResults = fetchWord(URLEncoder.encode(link.attributes().get("href"), StandardCharsets.UTF_8), link.text());
                                        if (!partialResults.isEmpty()) {
                                            results.addAll(partialResults);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error getting results from search", e);
        }

        return results;
    }

    private List<RaeResult> fetchWord(String link, String word) {
        List<RaeResult> results = new ArrayList<>();

        String completeURL;
        try {
            completeURL = BASEURL + link;

            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            Document document = Jsoup.parse(body.string());
                            Element article = document.getElementsByTag("article").first();
                            String articleId = null;
                            if (article != null) {
                                articleId = article.attributes().get("id");
                            }
                            Elements elements = document.select(".j");

                            if (!elements.isEmpty()) {
                                results = getResultsFromExactMatch(elements, word, articleId);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Fetching words", e);
        }

        return results;
    }

    private List<RaeResult> getResultsFromExactMatch(Elements elements, String word, String link) {
        List<RaeResult> results = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            RaeResult result = new RaeResult();
            if (link != null && !link.isEmpty()) {
                result.link = WORDLINKBYID + link;
            }
            result.index = i;
            result.word = capitalizeFirstLetter(word);
            Elements tags = element.getElementsByTag("abbr");
            tags.removeIf(x -> !Objects.equals(x.parent(), element));
            for (Element tag : tags) {
                result.tags.put(tag.text(), tag.attributes().get("title"));
            }
            Elements definition = element.getElementsByTag("mark");
            definition.removeIf(x -> !Objects.equals(x.parent(), element));
            if (definition.isEmpty()) {
                results.addAll(findResultsFromRedirect(element, word));
            } else {
                StringBuilder definitionBuilder = new StringBuilder();
                definition.forEach(y -> {
                    String partialText = y.text();
                    if (!definitionBuilder.isEmpty()) {
                        definitionBuilder.append(" ");
                        partialText = partialText.toLowerCase();
                    }
                    definitionBuilder.append(partialText);
                });
                result.definition = capitalizeFirstLetter(definitionBuilder.toString());
                results.add(result);
            }
        }

        return results;
    }

    private List<RaeResult> findResultsFromRedirect(Element element, String word) {
        List<RaeResult> results = new ArrayList<>();
        Element redirect = element.getElementsByTag("a").first();
        if (redirect != null) {
            String link = redirect.attributes().get("href");
            results = fetchWord(link, word);
        }

        return results;
    }

    private static String capitalizeFirstLetter(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public static class RaeResult {
        public int index;
        public String word;
        public Map<String, String> tags = new HashMap<>();
        public String definition;
        public String link;

        public String getDefinition() {
            final StringBuilder builder = new StringBuilder();
            if (link != null && !link.isEmpty()) {
                builder.append("[").append(word).append("](");
                builder.append(link).append(")\n");
            }
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                builder.append("*").append(tag.getKey()).append("*");
                builder.append(" (_").append(tag.getValue()).append("_)\n");
            }
            builder.append(definition);
            return builder.toString();
        }

        public String getDescription() {
            return definition;
        }

        public String getTitle() {
            final StringBuilder builder = new StringBuilder();
            builder.append(index).append(". ").append(word);
            return builder.toString();
        }
    }
}
