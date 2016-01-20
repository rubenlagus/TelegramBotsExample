package org.telegram.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Rae service
 * @date 20 of June of 2015
 */
public class RaeService {
    private static final String LOGTAG = "RAESERVICE";

    private static final String BASEURL = "http://dle.rae.es/srv/"; ///< Base url for REST
    private static final String SEARCHEXACTURL = "search?m=30&w=";
    private static final String SEARCHWORDURL = "search?m=form&w=";
    private static final String WORDLINKBYID = "http://dle.rae.es/?id=";


    public List<RaeResult> getResults(String query) {
        List<RaeResult> results = new ArrayList<>();

        String completeURL;
        try {
            completeURL = BASEURL + SEARCHEXACTURL + URLEncoder.encode(query, "UTF-8");

            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(completeURL);

            CloseableHttpResponse response = client.execute(request);
            HttpEntity ht = response.getEntity();

            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            String responseString = EntityUtils.toString(buf, "UTF-8");

            Document document = Jsoup.parse(responseString);
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
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }

        return results;
    }

    private List<RaeResult> getResultsWordSearch(String query) {
        List<RaeResult> results = new ArrayList<>();

        String completeURL;
        try {
            completeURL = BASEURL + SEARCHWORDURL + URLEncoder.encode(query, "UTF-8");

            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(completeURL);

            CloseableHttpResponse response = client.execute(request);
            HttpEntity ht = response.getEntity();

            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            String responseString = EntityUtils.toString(buf, "UTF-8");

            Document document = Jsoup.parse(responseString);
            Element list = document.select("body div ul").first();

            if (list != null) {
                Elements links = list.getElementsByTag("a");
                if (!links.isEmpty()) {
                    for (Element link : links) {
                        List<RaeResult> partialResults = fetchWord(URLEncoder.encode(link.attributes().get("href"), "UTF-8"), link.text());
                        if (!partialResults.isEmpty()) {
                            results.addAll(partialResults);
                        }
                    }
                }
            }
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }

        return results;
    }

    private List<RaeResult> fetchWord(String link, String word) {
        List<RaeResult> results = new ArrayList<>();

        String completeURL;
        try {
            completeURL = BASEURL + link;

            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(completeURL);

            CloseableHttpResponse response = client.execute(request);
            HttpEntity ht = response.getEntity();

            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            String responseString = EntityUtils.toString(buf, "UTF-8");

            Document document = Jsoup.parse(responseString);
            Element article = document.getElementsByTag("article").first();
            String articleId = null;
            if (article != null) {
                articleId = article.attributes().get("id");
            }
            Elements elements = document.select(".j");

            if (!elements.isEmpty()) {
                results = getResultsFromExactMatch(elements, word, articleId);
            }
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
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
            tags.removeIf(x -> !x.parent().equals(element));
            for (Element tag : tags) {
                result.tags.put(tag.text(), tag.attributes().get("title"));
            }
            Elements definition = element.getElementsByTag("mark");
            definition.removeIf(x -> !x.parent().equals(element));
            if (definition.isEmpty()) {
                results.addAll(findResultsFromRedirect(element, word));
            } else {
                StringBuilder definitionBuilder = new StringBuilder();
                definition.stream().forEachOrdered(y -> {
                    String partialText = y.text();
                    if (definitionBuilder.length() > 0) {
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
        if (original == null || original.length() == 0) {
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
