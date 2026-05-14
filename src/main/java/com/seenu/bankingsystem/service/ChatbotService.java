package com.seenu.bankingsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    /**
     * Banking-focused system prompt injected as the very first user turn.
     * Kept intentionally broad so follow-up messages in context aren't rejected.
     */
    private static final String SYSTEM_CONTEXT =
            "You are BMS Assistant, a smart and friendly banking assistant for BMS (Banking Management System). " +
            "You help users with: account balances, fund transfers, transaction history, beneficiaries, loans, " +
            "EMI calculations, password resets, login issues, branch info, and general banking FAQs. " +
            "\n\nFormatting rules:\n" +
            "- Keep responses concise and to the point (max 5-6 lines unless a list is needed).\n" +
            "- Use numbered lists (1. 2. 3.) when describing steps or multiple options.\n" +
            "- Use **bold** for important terms or action labels.\n" +
            "- Do NOT use headers (#, ##). Do NOT use bullet dashes (-). Do NOT use code blocks.\n" +
            "- End with a short follow-up question when helpful (e.g., 'Would you like help with anything else?').\n" +
            "\nBMS-specific context:\n" +
            "- Transaction History: Users can view it via Dashboard > Transactions page, filtering by date/type.\n" +
            "- Fund Transfer: Dashboard > Transfer. Requires beneficiary to be added first under Beneficiaries.\n" +
            "- Account Balance: Shown on the Dashboard and Accounts page after login.\n" +
            "- Loans: Users can apply for loans and view EMI schedules under the Loans section.\n" +
            "- Password Reset: Login page > Forgot Password, then verify OTP sent to registered email.\n" +
            "- If a question is clearly unrelated to banking or finance, politely decline and redirect back to banking topics.";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param userMessage  the latest message from the user
     * @param history      previous turns: [{role:"user"|"model", text:"..."}]
     */
    public String chat(String userMessage, List<Map<String, String>> history) throws Exception {

        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new RuntimeException("Gemini API key is not configured in application.properties");
        }

        // Build the multi-turn contents array for Gemini
        List<Map<String, Object>> contents = new ArrayList<>();

        // First turn: inject system context as a user message + a model acknowledgement
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", SYSTEM_CONTEXT))
        ));
        contents.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text", "Understood! I'm your BMS banking assistant. How can I help you today?"))
        ));

        // Append conversation history
        if (history != null) {
            for (Map<String, String> turn : history) {
                String role = turn.getOrDefault("role", "user");
                String text = turn.getOrDefault("text", "");
                if (!text.isBlank()) {
                    contents.add(Map.of(
                            "role", role,
                            "parts", List.of(Map.of("text", text))
                    ));
                }
            }
        }

        // Append the current user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.6,
                        "maxOutputTokens", 700
                )
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        log.debug("Sending to Gemini ({} turns): {}", contents.size(), jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + "?key=" + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Gemini response status: {}", response.statusCode());

        if (response.statusCode() != 200) {
            log.error("Gemini API error [{}]: {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API returned " + response.statusCode() + ": " + response.body());
        }

        Map<?, ?> parsed     = objectMapper.readValue(response.body(), Map.class);
        List<?>   candidates = (List<?>) parsed.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            log.error("No candidates in Gemini response: {}", response.body());
            throw new RuntimeException("Gemini returned no candidates");
        }
        Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        List<?>   parts   = (List<?>) content.get("parts");
        return (String) ((Map<?, ?>) parts.get(0)).get("text");
    }
}
