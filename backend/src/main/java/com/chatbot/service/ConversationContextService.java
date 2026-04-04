package com.chatbot.service;

import com.chatbot.dto.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConversationContextService {
    private static final Pattern COLLEGE_PATTERN = Pattern.compile(
            "(?:college name is|college is|my college is|for|at|of|from)\\s+([A-Za-z0-9 .,&()'-]{3,80})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GENERIC_COLLEGE_PATTERN = Pattern.compile(
            "([A-Za-z][A-Za-z0-9 .,&()'-]{1,60}\\s+(?:university|college|institute|campus))",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern COLLEGE_IN_QUERY_PATTERN = Pattern.compile(
            "\\b(?:in|at|for|from)\\s+([A-Za-z][A-Za-z0-9 .,&()'-]{1,60}\\s+(?:university|college|institute|campus))",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern COLLEGE_FREE_TEXT_QUERY_PATTERN = Pattern.compile(
            "\\b(?:in|at|for|from)\\s+([A-Za-z][A-Za-z0-9 .,&()'-]{2,60})$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern COURSE_FEE_PATTERN = Pattern.compile(
            "([A-Za-z][A-Za-z0-9 .+/-]{1,30})\\s+(?:fee|fees)\\s+(?:is|are|:)?\\s*([A-Za-z0-9 ,.+/-]{2,60})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern COURSE_QUERY_PATTERN = Pattern.compile(
            "(?:for|of|in|at)?\\s*([A-Za-z][A-Za-z0-9 .+&/-]{1,35})\\s+(?:fee|fees|cost|eligibility|documents|admission)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DIRECT_FEE_QUERY_PATTERN = Pattern.compile(
            "([A-Za-z][A-Za-z0-9 .+&/-]{1,40})\\s+(?:fee|fees|cost)$",
            Pattern.CASE_INSENSITIVE
    );

    public String buildConversationSummary(List<ChatMessage> history, String latestMessage) {
        List<String> lines = new ArrayList<>();
        String collegeName = extractCollegeName(history, latestMessage);
        if (collegeName != null) {
            lines.add("Selected college: " + collegeName);
        }

        List<String> knownFees = extractKnownFees(history, latestMessage);
        if (!knownFees.isEmpty()) {
            lines.add("User-provided fee details:");
            lines.addAll(knownFees);
        }

        List<String> userFacts = extractUsefulFacts(history);
        if (!userFacts.isEmpty()) {
            lines.add("Other user-provided admission details:");
            lines.addAll(userFacts);
        }

        return String.join("\n", lines);
    }

    public boolean hasCollegeContext(List<ChatMessage> history, String latestMessage) {
        return extractCollegeName(history, latestMessage) != null;
    }

    public String getCollegeName(List<ChatMessage> history, String latestMessage) {
        return extractCollegeName(history, latestMessage);
    }

    public String findCourseFee(List<ChatMessage> history, String latestMessage, String userMessage) {
        String requestedCourse = detectCourseName(userMessage);
        if (requestedCourse == null) {
            return null;
        }

        String fromLatest = findFeeForCourse(latestMessage, requestedCourse);
        if (fromLatest != null) {
            return fromLatest;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            if (!"user".equalsIgnoreCase(message.getRole())) {
                continue;
            }
            String fromHistory = findFeeForCourse(message.getContent(), requestedCourse);
            if (fromHistory != null) {
                return fromHistory;
            }
        }

        return null;
    }

    public String detectCourseName(String text) {
        String original = safe(text).trim();
        String lower = original.toLowerCase(Locale.ROOT);
        if (lower.contains("mbbs")) {
            return "MBBS";
        }
        if (lower.contains("bhms")) {
            return "BHMS";
        }
        if (lower.contains("bds")) {
            return "BDS";
        }
        if (lower.contains("bams")) {
            return "BAMS";
        }
        if (lower.contains("bpt")) {
            return "BPT";
        }
        if (lower.contains("mds")) {
            return "MDS";
        }
        if (lower.contains("md ") || lower.endsWith(" md") || lower.contains("m.d")) {
            return "MD";
        }
        if (lower.contains("ms ") || lower.endsWith(" ms") || lower.contains("m.s")) {
            return "MS";
        }
        if (lower.matches(".*\\bb\\.?(a|a\\.)\\b.*") || lower.matches(".*\\bba\\b.*")) {
            return "BA";
        }
        if (lower.matches(".*\\bb\\.?(com|com\\.)\\b.*") || lower.contains("bcom")) {
            return "B.Com";
        }
        if (lower.matches(".*\\bm\\.?(com|com\\.)\\b.*") || lower.contains("mcom")) {
            return "M.Com";
        }
        if (lower.contains("bsc") || lower.contains("b.sc")) {
            return "B.Sc";
        }
        if (lower.contains("msc") || lower.contains("m.sc")) {
            return "M.Sc";
        }
        if (lower.contains("ma ") || lower.endsWith(" ma") || lower.contains("m.a")) {
            return "MA";
        }
        if (lower.contains("llm")) {
            return "LLM";
        }
        if (lower.contains("llb")) {
            return "LLB";
        }
        if (lower.contains("bpharm") || lower.contains("b.pharm")) {
            return "B.Pharm";
        }
        if (lower.contains("mpharm") || lower.contains("m.pharm")) {
            return "M.Pharm";
        }
        if (lower.contains("bed") || lower.contains("b.ed")) {
            return "B.Ed";
        }
        if (lower.contains("med") || lower.contains("m.ed")) {
            return "M.Ed";
        }
        if (lower.contains("bhm") || lower.contains("hotel management")) {
            return "BHM";
        }
        if (lower.contains("mhm")) {
            return "MHM";
        }
        if (lower.contains("bjmc") || lower.contains("b.j.m.c")) {
            return "BJMC";
        }
        if (lower.contains("mca")) {
            return "MCA";
        }
        if (lower.contains("bca")) {
            return "BCA";
        }
        if (lower.contains("mba")) {
            return "MBA";
        }
        if (lower.contains("bba")) {
            return "BBA";
        }
        if (lower.contains("b.tech") || lower.contains("btech")) {
            return "B.Tech";
        }
        Matcher matcher = COURSE_QUERY_PATTERN.matcher(safe(text));
        if (matcher.find()) {
            String candidate = cleanupCourseName(removeLeadingCollegeWords(matcher.group(1)));
            if (isLikelyCourseName(candidate)) {
                return candidate;
            }
        }

        Matcher directMatcher = DIRECT_FEE_QUERY_PATTERN.matcher(original);
        if (directMatcher.find()) {
            String candidate = cleanupCourseName(removeLeadingCollegeWords(directMatcher.group(1)));
            if (isLikelyCourseName(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String extractCollegeName(List<ChatMessage> history, String latestMessage) {
        String fromLatest = findCollege(latestMessage);
        if (fromLatest != null) {
            return fromLatest;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            if (!"user".equalsIgnoreCase(message.getRole())) {
                continue;
            }
            String fromHistory = findCollege(message.getContent());
            if (fromHistory != null) {
                return fromHistory;
            }
        }
        return null;
    }

    private List<String> extractKnownFees(List<ChatMessage> history, String latestMessage) {
        List<String> fees = new ArrayList<>();
        collectFeeMentions(fees, latestMessage);
        for (ChatMessage message : history) {
            if ("user".equalsIgnoreCase(message.getRole())) {
                collectFeeMentions(fees, message.getContent());
            }
        }
        return fees.stream().distinct().toList();
    }

    private List<String> extractUsefulFacts(List<ChatMessage> history) {
        List<String> facts = new ArrayList<>();
        for (ChatMessage message : history) {
            if (!"user".equalsIgnoreCase(message.getRole())) {
                continue;
            }
            String content = safe(message.getContent());
            String lower = content.toLowerCase(Locale.ROOT);
            if (lower.contains("eligibility") || lower.contains("documents") || lower.contains("deadline")
                    || lower.contains("scholarship") || lower.contains("hostel") || lower.contains("admission process")) {
                facts.add("- " + trimForSummary(content));
            }
        }
        return facts.stream().distinct().limit(6).toList();
    }

    private String findCollege(String text) {
        Matcher inQueryMatcher = COLLEGE_IN_QUERY_PATTERN.matcher(safe(text));
        if (inQueryMatcher.find()) {
            return cleanupCollegeName(inQueryMatcher.group(1));
        }
        Matcher freeTextMatcher = COLLEGE_FREE_TEXT_QUERY_PATTERN.matcher(safe(text).trim());
        if (freeTextMatcher.find()) {
            return cleanupCollegeName(freeTextMatcher.group(1));
        }
        Matcher matcher = COLLEGE_PATTERN.matcher(safe(text));
        if (matcher.find()) {
            return cleanupCollegeName(matcher.group(1));
        }
        Matcher genericMatcher = GENERIC_COLLEGE_PATTERN.matcher(safe(text));
        if (genericMatcher.find()) {
            return cleanupCollegeName(genericMatcher.group(1));
        }
        return null;
    }

    private void collectFeeMentions(List<String> output, String text) {
        Matcher matcher = COURSE_FEE_PATTERN.matcher(safe(text));
        while (matcher.find()) {
            String feeValue = matcher.group(2).trim();
            if (looksLikeActualFee(feeValue)) {
                output.add("- " + matcher.group(1).trim() + ": " + feeValue);
            }
        }
    }

    private String trimForSummary(String text) {
        String cleaned = safe(text).trim();
        return cleaned.length() > 110 ? cleaned.substring(0, 107) + "..." : cleaned;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String findFeeForCourse(String text, String courseName) {
        Matcher matcher = COURSE_FEE_PATTERN.matcher(safe(text));
        while (matcher.find()) {
            String detectedCourse = matcher.group(1).trim();
            String feeValue = matcher.group(2).trim();
            if (normalizeCourse(detectedCourse).equals(normalizeCourse(courseName)) && looksLikeActualFee(feeValue)) {
                return feeValue;
            }
        }
        return null;
    }

    private String normalizeCourse(String course) {
        return safe(course).toLowerCase(Locale.ROOT).replace(".", "").replace(" ", "");
    }

    private String cleanupCourseName(String course) {
        String cleaned = safe(course).trim().replaceAll("\\s+", " ");
        cleaned = cleaned.replaceAll("^(what is the|what are the|tell me the|show me the)\\s+", "");
        return cleaned.replaceAll("[?.!,]+$", "");
    }

    private String removeLeadingCollegeWords(String text) {
        String cleaned = safe(text).trim();
        cleaned = cleaned.replaceAll("(?i)^.*\\b(university|college|institute|campus)\\b\\s+", "");
        return cleaned.trim();
    }

    private boolean isLikelyCourseName(String value) {
        String cleaned = safe(value).trim();
        if (cleaned.isBlank()) {
            return false;
        }
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.startsWith("what ") || lower.startsWith("is ") || lower.startsWith("the ")
                || lower.startsWith("for ") || lower.startsWith("of ")) {
            return false;
        }
        return cleaned.length() <= 20;
    }

    private String cleanupCollegeName(String value) {
        String cleaned = safe(value).trim().replaceAll("[.?!,]+$", "");
        cleaned = cleaned.replaceAll("(?i)^what\\s+is\\s+the\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^what\\s+is\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^what\\s+are\\s+the\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^fees?\\s+(for|of)\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^fees?\\s+from\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^eligibility\\s+(for|of)\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^eligibility\\s+from\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^(ba|b\\.a|bcom|b\\.com|mcom|m\\.com|bjmc|bca|bba|mba|mca|btech|b\\.tech|bhms|mbbs|bds|bams|bpt|mds|md|ms|bsc|b\\.sc|msc|m\\.sc|ma|m\\.a|llb|llm|bpharm|b\\.pharm|mpharm|m\\.pharm|bed|b\\.ed|med|m\\.ed|bhm|mhm)\\s+(in|at|of)\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^(ba|b\\.a|bcom|b\\.com|mcom|m\\.com|bjmc|bca|bba|mba|mca|btech|b\\.tech|bhms|mbbs|bds|bams|bpt|mds|md|ms|bsc|b\\.sc|msc|m\\.sc|ma|m\\.a|llb|llm|bpharm|b\\.pharm|mpharm|m\\.pharm|bed|b\\.ed|med|m\\.ed|bhm|mhm)\\s+fees?\\s+from\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^(course|fees?|eligibility|documents?)\\s+(for|of|in)\\s+", "");
        cleaned = cleaned.replaceAll("(?i)^(is\\s+the\\s+fees\\s+of\\s+)", "");
        cleaned = cleaned.replaceAll("(?i)^(is\\s+the\\s+eligibility\\s+of\\s+)", "");
        cleaned = cleaned.replaceAll("(?i)^(bhms|mbbs|bds|bams|bpt|mds|md|ms|ba|b\\.a|bcom|b\\.com|mcom|m\\.com|bsc|b\\.sc|msc|m\\.sc|ma|m\\.a|bjmc|bca|bba|mba|mca|btech|b\\.tech|llb|llm|bpharm|b\\.pharm|mpharm|m\\.pharm|bed|b\\.ed|med|m\\.ed|bhm|mhm)\\s*[-:]\\s*", "");
        return cleaned.trim();
    }

    private boolean looksLikeActualFee(String value) {
        String lower = safe(value).toLowerCase(Locale.ROOT);
        return lower.matches(".*\\d.*")
                || lower.contains("rs")
                || lower.contains("inr")
                || lower.contains("lakh")
                || lower.contains("lac")
                || lower.contains("per year")
                || lower.contains("per semester")
                || lower.contains("annum");
    }
}
