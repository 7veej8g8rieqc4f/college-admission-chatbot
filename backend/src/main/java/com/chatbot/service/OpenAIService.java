package com.chatbot.service;

import com.chatbot.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AdmissionKnowledgeService admissionKnowledgeService;
    private final LiveAdmissionLookupService liveAdmissionLookupService;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.timeoutMs:60000}")
    private long timeoutMs;

    public OpenAIService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AdmissionKnowledgeService admissionKnowledgeService,
            LiveAdmissionLookupService liveAdmissionLookupService
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.admissionKnowledgeService = admissionKnowledgeService;
        this.liveAdmissionLookupService = liveAdmissionLookupService;
    }

    public String getChatResponse(String userMessage, List<ChatMessage> history) {
        String trimmedKey = apiKey == null ? null : apiKey.trim();
        String liveLookupAnswer = liveAdmissionLookupService.lookupAdmissionAnswer(userMessage, history);

        if (!hasConfiguredApiKey(trimmedKey)) {
            if (liveLookupAnswer != null && !liveLookupAnswer.isBlank()) {
                return sanitizeReply(userMessage, history, liveLookupAnswer);
            }
            return sanitizeReply(userMessage, history, admissionKnowledgeService.getRuleBasedResponse(userMessage, history));
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            String systemPrompt = admissionKnowledgeService.buildSystemPrompt(history, userMessage);
            if (liveLookupAnswer != null && !liveLookupAnswer.isBlank()) {
                systemPrompt = systemPrompt + "\n\nCurrent web lookup context:\n" + liveLookupAnswer
                        + "\nUse this web context when it helps. If web details look uncertain, say that briefly.";
            }
            messages.add(Map.of("role", "system", "content", systemPrompt));
            for (ChatMessage item : history) {
                if (item == null || item.getRole() == null || item.getContent() == null) {
                    continue;
                }
                if (!item.getRole().equalsIgnoreCase("user") && !item.getRole().equalsIgnoreCase("assistant")) {
                    continue;
                }
                messages.add(Map.of(
                        "role", item.getRole().toLowerCase(),
                        "content", item.getContent()
                ));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", 0.2
            );

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + trimmedKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (liveLookupAnswer != null && !liveLookupAnswer.isBlank()) {
                    return sanitizeReply(userMessage, history, liveLookupAnswer);
                }
                return sanitizeReply(userMessage, history, admissionKnowledgeService.getRuleBasedResponse(userMessage, history));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content");

            if (content.isMissingNode() || content.isNull()) {
                if (liveLookupAnswer != null && !liveLookupAnswer.isBlank()) {
                    return sanitizeReply(userMessage, history, liveLookupAnswer);
                }
                return sanitizeReply(userMessage, history, admissionKnowledgeService.getRuleBasedResponse(userMessage, history));
            }

            return sanitizeReply(userMessage, history, content.asText());
        } catch (Exception e) {
            if (liveLookupAnswer != null && !liveLookupAnswer.isBlank()) {
                return sanitizeReply(userMessage, history, liveLookupAnswer);
            }
            return sanitizeReply(userMessage, history, admissionKnowledgeService.getRuleBasedResponse(userMessage, history));
        }
    }

    private boolean hasConfiguredApiKey(String trimmedKey) {
        return trimmedKey != null
                && !trimmedKey.isBlank()
                && !trimmedKey.startsWith("PUT_YOUR")
                && !trimmedKey.equalsIgnoreCase("sk-your-key-here");
    }

    private String sanitizeReply(String userMessage, List<ChatMessage> history, String reply) {
        if (reply == null || reply.isBlank()) {
            return admissionKnowledgeService.getRuleBasedResponse(userMessage, history);
        }

        String normalizedReply = normalize(reply);
        String normalizedUser = normalize(userMessage);
        if (normalizedReply.equals(normalizedUser) || normalizedReply.contains(normalizedUser)) {
            String courseName = admissionKnowledgeService.extractCourseName(userMessage);
            String collegeName = admissionKnowledgeService.extractCollegeName(history, userMessage);
            if (courseName != null && collegeName != null) {
                return "You asked about " + courseName + " at " + collegeName
                        + ". Please ask about eligibility, documents, last date, scholarship, or admission process.";
            }
            if (courseName != null) {
                return "You asked about " + courseName
                        + ". Please ask about eligibility, documents, last date, scholarship, or admission process.";
            }
            return "Please ask a clear admission question about eligibility, documents, last date, scholarship, or admission process.";
        }
        return reply;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }
}
