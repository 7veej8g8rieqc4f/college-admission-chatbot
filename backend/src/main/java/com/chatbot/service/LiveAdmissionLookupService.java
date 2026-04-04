package com.chatbot.service;

import com.chatbot.dto.ChatMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LiveAdmissionLookupService {
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?i)(?:rs\\.?|inr|₹)\\s*[0-9][0-9,./ ]{1,20}(?:\\s*(?:lakh|lakhs|crore|crores))?(?:\\s*(?:per year|per annum|per semester|annum))?"
                    + "|[0-9]+(?:\\.[0-9]+)?\\s*(?:lakh|lakhs|crore|crores)(?:\\s*(?:per year|per annum|per semester|annum))?"
    );
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+|\\s{2,}|\\n+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final HttpClient httpClient;
    private final ConversationContextService conversationContextService;

    @Value("${live.lookup.enabled:true}")
    private boolean liveLookupEnabled;

    @Value("${live.lookup.search-url:https://search.yahoo.com/search?p=}")
    private String searchUrl;

    @Value("${live.lookup.timeoutMs:12000}")
    private long timeoutMs;

    public LiveAdmissionLookupService(
            HttpClient httpClient,
            ConversationContextService conversationContextService
    ) {
        this.httpClient = httpClient;
        this.conversationContextService = conversationContextService;
    }

    public String lookupAdmissionAnswer(String userMessage, List<ChatMessage> history) {
        if (!liveLookupEnabled || userMessage == null || userMessage.isBlank()) {
            return null;
        }

        String normalized = userMessage.toLowerCase(Locale.ROOT);
        if (!looksLikeAdmissionLookup(normalized)) {
            return null;
        }

        try {
            String collegeName = conversationContextService.getCollegeName(history, userMessage);
            String courseName = conversationContextService.detectCourseName(userMessage);
            String topic = detectTopic(normalized);

            List<SearchResult> results = search(userMessage, history, collegeName, courseName);
            if (results.isEmpty()) {
                return null;
            }

            if ("fee".equals(topic)) {
                String feeAnswer = extractFeeAnswer(results, courseName, collegeName);
                if (feeAnswer != null) {
                    return feeAnswer;
                }
                return fallbackFeeAnswer(courseName, collegeName, results);
            }

            if ("eligibility".equals(topic)) {
                String eligibilityAnswer = extractEligibilityAnswer(results, courseName, collegeName);
                if (eligibilityAnswer != null) {
                    return eligibilityAnswer;
                }
            }

            return summarizeResults(topic, courseName, collegeName, results);
        } catch (Exception e) {
            return null;
        }
    }

    private List<SearchResult> search(String userMessage, List<ChatMessage> history, String collegeName, String courseName) throws Exception {
        String query = buildSearchQuery(userMessage, history, collegeName, courseName);
        String html = sendGet(searchUrl + URLEncoder.encode(query, StandardCharsets.UTF_8));
        if (html == null || html.isBlank()) {
            return List.of();
        }

        Document doc = Jsoup.parse(html);
        Elements links = doc.select("a.result__a");
        Elements snippets = doc.select(".result__snippet");

        List<SearchResult> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < links.size(); i++) {
            Element link = links.get(i);
            String url = decodeDuckDuckGoUrl(link.attr("href"));
            if (url == null || url.isBlank() || !seen.add(url)) {
                continue;
            }
            String title = clean(link.text());
            String snippet = i < snippets.size() ? clean(snippets.get(i).text()) : "";
            results.add(new SearchResult(title, url, snippet, scoreResult(url, title, snippet, collegeName, courseName)));
            if (results.size() >= 6) {
                break;
            }
        }
        results.sort(Comparator.comparingInt(SearchResult::score).reversed());
        return results;
    }

    private String extractFeeAnswer(List<SearchResult> results, String courseName, String collegeName) {
        for (SearchResult result : results) {
            String sourceText = fetchPageText(result.url());
            if (sourceText == null || sourceText.isBlank()) {
                sourceText = result.snippet();
            }
            String sentence = findRelevantSentence(sourceText, courseName, "fee", "fees", "tuition", "semester fee", "annual fee");
            if (sentence == null) {
                continue;
            }
            Matcher moneyMatcher = MONEY_PATTERN.matcher(sentence);
            if (moneyMatcher.find()) {
                String fee = moneyMatcher.group().trim();
                String prefix = buildSubject(courseName, collegeName);
                return prefix + " fee is " + fee + ". Source: " + result.title() + ".";
            }
        }
        return null;
    }

    private String extractEligibilityAnswer(List<SearchResult> results, String courseName, String collegeName) {
        for (SearchResult result : results) {
            String sourceText = fetchPageText(result.url());
            if (sourceText == null || sourceText.isBlank()) {
                sourceText = result.snippet();
            }
            String sentence = findRelevantSentence(sourceText, courseName, "eligibility", "eligible", "minimum", "qualification");
            if (sentence != null) {
                String prefix = buildSubject(courseName, collegeName);
                return prefix + " eligibility: " + trimSentence(sentence) + " Source: " + result.title() + ".";
            }
        }
        return null;
    }

    private String summarizeResults(String topic, String courseName, String collegeName, List<SearchResult> results) {
        List<String> lines = new ArrayList<>();
        lines.add("Based on current web results for " + topic + " in " + buildSubject(courseName, collegeName) + ":");
        int added = 0;
        for (SearchResult result : results) {
            if (result.snippet().isBlank()) {
                continue;
            }
            lines.add("- " + result.title() + ": " + result.snippet());
            added++;
            if (added >= 2) {
                break;
            }
        }
        if (added == 0) {
            return null;
        }
        lines.add("Please verify the latest details on the official admissions page.");
        return String.join(" ", lines);
    }

    private String fallbackFeeAnswer(String courseName, String collegeName, List<SearchResult> results) {
        if (!results.isEmpty()) {
            List<String> parts = new ArrayList<>();
            parts.add("I could not confirm one exact latest " + (courseName == null ? "" : courseName + " ") + "fee");
            if (collegeName != null) {
                parts.add("for " + collegeName + " from the available web pages.");
            } else {
                parts.add("from the available web pages.");
            }

            String approximateFee = findApproximateFeeFromResults(results);
            if (approximateFee != null) {
                parts.add("An approximate fee based on current web results is " + approximateFee + ".");
            }

            int added = 0;
            for (SearchResult result : results) {
                StringBuilder line = new StringBuilder();
                line.append("Source ").append(added + 1).append(": ").append(result.title());
                if (result.url() != null && !result.url().isBlank()) {
                    line.append(" (").append(result.url()).append(")");
                }
                if (result.snippet() != null && !result.snippet().isBlank()) {
                    line.append(" - ").append(trimSentence(result.snippet()));
                }
                parts.add(line.toString());
                added++;
                if (added >= 2) {
                    break;
                }
            }
            parts.add("Please open the source links and verify the latest fee on the official admissions page.");
            return String.join(" ", parts);
        }

        if (collegeName != null) {
            return "I could not fetch an exact " + (courseName == null ? "" : courseName + " ") + "fee for " + collegeName
                    + " right now. Please check the official admissions page"
                    + (results.isEmpty() ? "." : " or the top web results for the latest fee.");
        }
        if (courseName != null) {
            return "I could not fetch an exact " + courseName + " fee right now. Please share the college name for a more specific lookup.";
        }
        return null;
    }

    private String findApproximateFeeFromResults(List<SearchResult> results) {
        for (SearchResult result : results) {
            String snippet = result.snippet();
            if (snippet == null || snippet.isBlank()) {
                continue;
            }
            Matcher matcher = MONEY_PATTERN.matcher(snippet);
            if (matcher.find()) {
                return matcher.group().trim();
            }
        }
        return approximateByCourse(results);
    }

    private String approximateByCourse(List<SearchResult> results) {
        String corpus = results.stream()
                .map(result -> safe(result.title()) + " " + safe(result.snippet()))
                .reduce("", (left, right) -> left + " " + right);

        if (corpus.contains("mba")) {
            return "around INR 1.5 lakh to 4 lakh per year";
        }
        if (corpus.contains("mca")) {
            return "around INR 1 lakh to 3 lakh per year";
        }
        if (corpus.contains("bcom") || corpus.contains("b com") || corpus.contains("mcom") || corpus.contains("m com")) {
            return "around INR 60,000 to 2 lakh per year";
        }
        if (corpus.contains("bjmc")) {
            return "around INR 80,000 to 2.5 lakh per year";
        }
        if (corpus.contains("bsc") || corpus.contains("b sc") || corpus.contains("msc") || corpus.contains("m sc")) {
            return "around INR 50,000 to 2.5 lakh per year";
        }
        if (corpus.contains("ba")) {
            return "around INR 40,000 to 1.5 lakh per year";
        }
        return null;
    }

    private String fetchPageText(String url) {
        try {
            String html = sendGet(url);
            if (html == null || html.isBlank()) {
                return null;
            }
            Document document = Jsoup.parse(html);
            document.select("script,style,noscript,svg,header,footer,nav").remove();
            return clean(document.text());
        } catch (Exception e) {
            return null;
        }
    }

    private String findRelevantSentence(String text, String courseName, String... keywords) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] parts = SENTENCE_SPLIT_PATTERN.split(text);
        String normalizedCourse = courseName == null ? null : courseName.toLowerCase(Locale.ROOT).replace(".", "");
        for (String part : parts) {
            String cleaned = trimSentence(part);
            if (cleaned.length() < 20 || cleaned.length() > 260) {
                continue;
            }
            String lower = cleaned.toLowerCase(Locale.ROOT);
            if (normalizedCourse != null && !lower.contains(normalizedCourse.toLowerCase(Locale.ROOT).replace(".", ""))) {
                continue;
            }
            boolean hasKeyword = false;
            for (String keyword : keywords) {
                if (lower.contains(keyword)) {
                    hasKeyword = true;
                    break;
                }
            }
            if (!hasKeyword) {
                continue;
            }
            return cleaned;
        }
        return null;
    }

    private String sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("User-Agent", "Mozilla/5.0 AdmissionHelpDeskBot/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }
        return response.body();
    }

    private String buildSearchQuery(String userMessage, List<ChatMessage> history, String collegeName, String courseName) {
        StringBuilder query = new StringBuilder(userMessage.trim());
        if (collegeName != null && !userMessage.toLowerCase(Locale.ROOT).contains(collegeName.toLowerCase(Locale.ROOT))) {
            query.append(" ").append(collegeName);
        }
        if (courseName != null && !userMessage.toLowerCase(Locale.ROOT).contains(courseName.toLowerCase(Locale.ROOT))) {
            query.append(" ").append(courseName);
        }
        query.append(" official admissions");
        return query.toString();
    }

    private int scoreResult(String url, String title, String snippet, String collegeName, String courseName) {
        int score = 0;
        String lowerUrl = safe(url);
        String lowerTitle = safe(title);
        String lowerSnippet = safe(snippet);
        if (collegeName != null && containsNormalized(lowerUrl, collegeName)) {
            score += 5;
        }
        if (collegeName != null && containsNormalized(lowerTitle, collegeName)) {
            score += 4;
        }
        if (courseName != null && containsNormalized(lowerTitle, courseName)) {
            score += 3;
        }
        if (courseName != null && containsNormalized(lowerSnippet, courseName)) {
            score += 2;
        }
        if (lowerUrl.contains(".edu") || lowerUrl.contains("/admission") || lowerUrl.contains("/admissions")) {
            score += 2;
        }
        if (lowerTitle.contains("admission") || lowerTitle.contains("fees") || lowerTitle.contains("eligibility")) {
            score += 1;
        }
        return score;
    }

    private boolean containsNormalized(String haystack, String needle) {
        String left = normalize(haystack);
        String right = normalize(needle);
        return !right.isBlank() && left.contains(right);
    }

    private boolean looksLikeAdmissionLookup(String normalized) {
        return containsAny(normalized,
                "eligibility", "document", "documents", "last date",
                "deadline", "admission", "apply", "scholarship", "counselling", "counseling",
                "hostel", "placement", "entrance", "fee", "fees", "cost", "tuition");
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String detectTopic(String normalized) {
        if (containsAny(normalized, "fee", "fees", "cost", "tuition")) {
            return "fee";
        }
        if (containsAny(normalized, "eligibility", "eligible")) {
            return "eligibility";
        }
        if (containsAny(normalized, "document", "documents")) {
            return "documents";
        }
        if (containsAny(normalized, "deadline", "last date", "apply")) {
            return "admission deadline";
        }
        return "admission";
    }

    private String buildSubject(String courseName, String collegeName) {
        if (courseName != null && collegeName != null) {
            return courseName + " at " + collegeName;
        }
        if (courseName != null) {
            return courseName;
        }
        if (collegeName != null) {
            return collegeName;
        }
        return "the requested course";
    }

    private String clean(String value) {
        return WHITESPACE_PATTERN.matcher(value == null ? "" : value).replaceAll(" ").trim();
    }

    private String trimSentence(String value) {
        String cleaned = clean(value);
        if (cleaned.length() > 220) {
            return cleaned.substring(0, 217) + "...";
        }
        return cleaned;
    }

    private String decodeDuckDuckGoUrl(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.replace("&amp;", "&");
        int uddgIndex = url.indexOf("uddg=");
        if (uddgIndex >= 0) {
            String encoded = url.substring(uddgIndex + 5);
            int nextAmp = encoded.indexOf('&');
            if (nextAmp >= 0) {
                encoded = encoded.substring(0, nextAmp);
            }
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        }
        return url;
    }

    private String normalize(String value) {
        return safe(value).replace(".", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record SearchResult(String title, String url, String snippet, int score) {
    }
}
