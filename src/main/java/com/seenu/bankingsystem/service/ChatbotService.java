package com.seenu.bankingsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenu.bankingsystem.entity.Account;
import com.seenu.bankingsystem.entity.Branch;
import com.seenu.bankingsystem.entity.Loan;
import com.seenu.bankingsystem.entity.User;
import com.seenu.bankingsystem.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final String GEMINI_STREAM_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent";

    private static final String SYSTEM_CONTEXT =
            "You are BMS Assistant, a smart and friendly banking assistant for BMS (Banking Management System). " +
            "You help users with: account balances, fund transfers, transaction history, spending insights, transaction categorization, beneficiaries, loans, " +
            "EMI calculations, password resets, login issues, branch info, and general banking FAQs. " +
            "\n\nFormatting rules:\n" +
            "- Keep responses concise and to the point (max 5-6 lines unless a list is needed).\n" +
            "- Use numbered lists (1. 2. 3.) when describing steps or multiple options.\n" +
            "- Use **bold** for important terms or action labels.\n" +
            "- Do NOT use headers (#, ##). Do NOT use bullet dashes (-). Do NOT use code blocks.\n" +
            "- End with a short follow-up question when helpful (e.g., 'Would you like help with anything else?').\n" +
            "\nBMS-specific context:\n" +
            "- Transaction History: Users can view it via Dashboard > Transactions page, filtering by date/type.\n" +
            "- Spending Insights: Users can see their visual spending breakdown directly on their Dashboard. You can also retrieve their monthly category spending, totals, and variances by calling the getSpendingInsights tool.\n" +
            "- Fund Transfer: Dashboard > Transfer. Requires beneficiary to be added first under Beneficiaries.\n" +
            "- Account Balance: Shown on the Dashboard and Accounts page after login.\n" +
            "- Loans: Users can apply for loans and view EMI schedules under the Loans section.\n" +
            "- Password Reset: Login page > Forgot Password, then verify OTP sent to registered email.\n" +
            "- Always use the local banking tools/functions provided to get real-time balances, recent transactions, active loans, spending insights, and branch details if the user asks.\n" +
            "- If the user asks for balance, transactions, loans, or spending insights, and the tools return an authentication error (user not logged in), politely ask them to log in first.\n" +
            "- If a question is clearly unrelated to banking or finance, politely decline and redirect back to banking topics.";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;


    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private BranchService branchService;

    /**
     * Standard non-streaming chat with function calling.
     */
    public String chat(String userMessage, List<Map<String, String>> history) throws Exception {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new RuntimeException("Gemini API key is not configured in application.properties");
        }

        List<Map<String, Object>> contents = new ArrayList<>();

        // 1. Inject system context
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", SYSTEM_CONTEXT))
        ));
        contents.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text", "Understood! I'm your BMS banking assistant. How can I help you today?"))
        ));

        // 2. Add history
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

        // 3. Add current message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> requestBody = new HashMap<>(Map.of(
                "contents", contents,
                "tools", buildToolsDefinition(),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 800
                )
        ));

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpResponse<String> response = sendPostRequest(GEMINI_URL, jsonBody);

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API returned " + response.statusCode() + ": " + response.body());
        }

        Map<?, ?> parsed = objectMapper.readValue(response.body(), Map.class);
        List<?> candidates = (List<?>) parsed.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Gemini returned no candidates");
        }

        Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
        Map<?, ?> content = (Map<?, ?>) candidate.get("content");
        List<?> parts = (List<?>) content.get("parts");
        Map<?, ?> part = (Map<?, ?>) parts.get(0);

        if (part.containsKey("functionCall")) {
            // Handle Tool Call
            Map<?, ?> functionCall = (Map<?, ?>) part.get("functionCall");
            String functionName = (String) functionCall.get("name");
            Map<?, ?> args = (Map<?, ?>) functionCall.get("args");

            Map<String, Object> toolResult = executeTool(functionName, args);

            // Append function call block to content history
            contents.add(Map.of(
                    "role", "model",
                    "parts", List.of(Map.of("functionCall", functionCall))
            ));

            // Append function response turn to content history
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("functionResponse", Map.of(
                            "name", functionName,
                            "response", Map.of("output", toolResult)
                    )))
            ));

            // Request Gemini again with the tool result included
            Map<String, Object> followUpRequestBody = Map.of(
                    "contents", contents,
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 800
                    )
            );

            String followUpJson = objectMapper.writeValueAsString(followUpRequestBody);
            HttpResponse<String> followUpResponse = sendPostRequest(GEMINI_URL, followUpJson);

            if (followUpResponse.statusCode() != 200) {
                throw new RuntimeException("Gemini Follow-up API returned " + followUpResponse.statusCode());
            }

            Map<?, ?> followUpParsed = objectMapper.readValue(followUpResponse.body(), Map.class);
            List<?> followUpCandidates = (List<?>) followUpParsed.get("candidates");
            Map<?, ?> finalContent = (Map<?, ?>) ((Map<?, ?>) followUpCandidates.get(0)).get("content");
            List<?> finalParts = (List<?>) finalContent.get("parts");
            return (String) ((Map<?, ?>) finalParts.get(0)).get("text");
        }

        return (String) part.get("text");
    }

    /**
     * Real-time Token Streaming via SSE (Server-Sent Events) with Tool/Function Calling.
     */
    public void chatStream(String userMessage, List<Map<String, String>> history, SseEmitter emitter) {
        try {
            if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
                emitter.send(SseEmitter.event().name("error").data("Gemini API key is not configured."));
                emitter.complete();
                return;
            }

            List<Map<String, Object>> contents = new ArrayList<>();

            // 1. System instruction turns
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", SYSTEM_CONTEXT))
            ));
            contents.add(Map.of(
                    "role", "model",
                    "parts", List.of(Map.of("text", "Understood! I'm your BMS banking assistant. How can I help you today?"))
            ));

            // 2. Chat history
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

            // 3. User message
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
            ));

            Map<String, Object> requestBody = new HashMap<>(Map.of(
                    "contents", contents,
                    "tools", buildToolsDefinition(),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 800
                    )
            ));

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.info("Starting Gemini request stream...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_STREAM_URL + "?alt=sse&key=" + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<InputStream> response = sendStreamRequest(request);

            if (response.statusCode() != 200) {
                String errorMsg = response.statusCode() == 429
                        ? "BMS Assistant is experiencing heavy traffic right now. Please try again in a few moments."
                        : "BMS Assistant is temporarily unavailable. Please try again later.";
                emitter.send(SseEmitter.event().data(errorMsg));
                emitter.complete();
                try {
                    response.body().close();
                } catch (Exception ignored) {}
                return;
            }

            boolean isFunctionCallRequested = false;
            String functionName = null;
            Map<String, Object> functionArgs = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String jsonChunk = line.substring(6).trim();
                        if (jsonChunk.equals("[DONE]") || jsonChunk.isBlank()) continue;

                        Map<?, ?> chunkFn = extractFunctionCallFromChunk(jsonChunk);
                        if (chunkFn != null) {
                            isFunctionCallRequested = true;
                            functionName = (String) chunkFn.get("name");
                            Object argsObj = chunkFn.get("args");
                            if (argsObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> argsMap = (Map<String, Object>) argsObj;
                                functionArgs.putAll(argsMap);
                            }
                        } else if (!isFunctionCallRequested) {
                            // Standard Text stream
                            String text = extractTextFromChunk(jsonChunk);
                            if (text != null) {
                                emitter.send(SseEmitter.event().data(text));
                            }
                        }
                    }
                }
            }

            // If a tool call was detected, execute it and perform a follow-up streaming request
            if (isFunctionCallRequested && functionName != null) {
                log.info("Function call requested by Gemini in stream: {} with args: {}", functionName, functionArgs);
                Map<String, Object> toolResult = executeTool(functionName, functionArgs);

                // Add tools interaction to conversational turn history
                contents.add(Map.of(
                        "role", "model",
                        "parts", List.of(Map.of("functionCall", Map.of(
                                "name", functionName,
                                "args", functionArgs
                        )))
                ));

                contents.add(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("functionResponse", Map.of(
                                "name", functionName,
                                "response", Map.of("output", toolResult)
                        )))
                ));

                // Re-request Gemini stream with tool result injected
                Map<String, Object> followUpRequestBody = Map.of(
                        "contents", contents,
                        "generationConfig", Map.of(
                                "temperature", 0.4,
                                "maxOutputTokens", 800
                        )
                );

                String followUpJson = objectMapper.writeValueAsString(followUpRequestBody);
                HttpRequest followUpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(GEMINI_STREAM_URL + "?alt=sse&key=" + geminiApiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(followUpJson))
                        .build();

                HttpResponse<InputStream> followUpResponse = sendStreamRequest(followUpRequest);

                if (followUpResponse.statusCode() != 200) {
                    String errorMsg = followUpResponse.statusCode() == 429
                            ? "BMS Assistant is experiencing heavy traffic right now. Please try again in a few moments."
                            : "BMS Assistant is temporarily unavailable. Please try again later.";
                    emitter.send(SseEmitter.event().data(errorMsg));
                    emitter.complete();
                    try {
                        followUpResponse.body().close();
                    } catch (Exception ignored) {}
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(followUpResponse.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String jsonChunk = line.substring(6).trim();
                            if (jsonChunk.equals("[DONE]") || jsonChunk.isBlank()) continue;

                            String text = extractTextFromChunk(jsonChunk);
                            if (text != null) {
                                emitter.send(SseEmitter.event().data(text));
                            }
                        }
                    }
                }
            }

            emitter.complete();

        } catch (Exception e) {
            log.error("Error during chatbot stream: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("error").data("An error occurred: " + e.getMessage()));
            } catch (Exception ignored) {}
            emitter.complete();
        }
    }

    private HttpResponse<String> sendPostRequest(String url, String jsonBody) throws Exception {
        int maxRetries = 3;
        int delayMs = 1500;
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "?key=" + geminiApiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429) {
                    log.warn("Gemini API returned 429 (Rate Limit). Retrying in {} ms... (Attempt {} of {})", delayMs, i + 1, maxRetries);
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                    continue;
                }
                return response;
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini API request failed. Retrying in {} ms... (Attempt {} of {})", delayMs, i + 1, maxRetries);
                Thread.sleep(delayMs);
                delayMs *= 2;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new RuntimeException("Gemini API returned 429 Rate Limit after retries.");
    }

    private HttpResponse<InputStream> sendStreamRequest(HttpRequest request) throws Exception {
        int maxRetries = 3;
        int delayMs = 1500;
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == 429) {
                    log.warn("Gemini Stream API returned 429 (Rate Limit). Retrying in {} ms... (Attempt {} of {})", delayMs, i + 1, maxRetries);
                    try {
                        response.body().close();
                    } catch (Exception ignored) {}
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                    continue;
                }
                return response;
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini Stream API request failed. Retrying in {} ms... (Attempt {} of {})", delayMs, i + 1, maxRetries);
                Thread.sleep(delayMs);
                delayMs *= 2;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new RuntimeException("Gemini Stream API returned 429 Rate Limit after retries.");
    }

    private List<Map<String, Object>> buildToolsDefinition() {
        Map<String, Object> getAccountDetails = Map.of(
                "name", "getAccountDetails",
                "description", "Fetch details of all active bank accounts belonging to the logged-in user, including account numbers, types, balances, and branches."
        );

        Map<String, Object> getRecentTransactions = Map.of(
                "name", "getRecentTransactions",
                "description", "Fetch the last 5 transactions for a specific account number belonging to the logged-in user.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "accountNumber", Map.of(
                                        "type", "STRING",
                                        "description", "The bank account number to fetch transactions for."
                                )
                        ),
                        "required", List.of("accountNumber")
                )
        );

        Map<String, Object> getActiveLoans = Map.of(
                "name", "getActiveLoans",
                "description", "Fetch all active loans for the logged-in user, including principal amount, EMI, and interest rate."
        );

        Map<String, Object> getBranchLocations = Map.of(
                "name", "getBranchLocations",
                "description", "Fetch physical bank branch offices and their addresses."
        );

        Map<String, Object> getSpendingInsights = Map.of(
                "name", "getSpendingInsights",
                "description", "Fetch monthly spending insights, expense categorizations (Food, Travel, Bills, Shopping, etc.), monthly variances, and budgets for the logged-in user."
        );

        return List.of(Map.of(
                "functionDeclarations", List.of(
                        getAccountDetails,
                        getRecentTransactions,
                        getActiveLoans,
                        getBranchLocations,
                        getSpendingInsights
                )
        ));
    }

    private Map<String, Object> executeTool(String name, Map<?, ?> args) {
        log.info("Executing local tool call: {} with arguments: {}", name, args);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return Map.of("error", "User is not logged in. Access denied for secure bank queries. Please tell the user to log in first.");
            }
            String username = auth.getName();
            User user = userService.findByEmail(username);

            switch (name) {
                case "getAccountDetails":
                    List<Account> accounts = accountRepository.findAllByUserId(user.getId());
                    List<Map<String, Object>> accountData = new ArrayList<>();
                    for (Account acc : accounts) {
                        accountData.add(Map.of(
                                "accountNumber", acc.getAccountNumber(),
                                "accountType", acc.getAccountType(),
                                "balance", acc.getBalance(),
                                "status", acc.getStatus(),
                                "branch", acc.getBranch()
                        ));
                    }
                    return Map.of("accounts", accountData);

                case "getRecentTransactions":
                    String accNum = (String) args.get("accountNumber");
                    if (accNum == null || accNum.isBlank()) {
                        return Map.of("error", "Missing required argument 'accountNumber'");
                    }
                    // Security verification: verify the account belongs to the logged-in user
                    Optional<Account> testAcc = accountRepository.findByAccountNumber(accNum);
                    if (testAcc.isEmpty() || !testAcc.get().getUserId().equals(user.getId())) {
                        return Map.of("error", "Unauthorized: This account number does not belong to you.");
                    }
                    var page = transactionService.getStatement(accNum, 0, 5);
                    return Map.of("transactions", page.getContent());

                case "getActiveLoans":
                    List<Loan> loans = loanService.getLoansForUser(user.getId());
                    List<Map<String, Object>> loanData = new ArrayList<>();
                    for (Loan loan : loans) {
                        loanData.add(Map.of(
                                "id", loan.getId(),
                                "amount", loan.getAmount(),
                                "emi", loan.getEmi(),
                                "outstandingBalance", loan.getOutstandingBalance(),
                                "status", loan.getStatus(),
                                "purpose", loan.getPurpose() != null ? loan.getPurpose() : "",
                                "interestRate", loan.getInterestRate()
                        ));
                    }
                    return Map.of("loans", loanData);

                case "getBranchLocations":
                    List<Branch> branches = branchService.getAllBranches();
                    List<Map<String, Object>> branchData = new ArrayList<>();
                    for (Branch br : branches) {
                        branchData.add(Map.of(
                                "name", br.getBranchName() != null ? br.getBranchName() : "",
                                "code", br.getBranchCode() != null ? br.getBranchCode() : "",
                                "location", br.getLocation() != null ? br.getLocation() : ""
                        ));
                    }
                    return Map.of("branches", branchData);

                case "getSpendingInsights":
                    return transactionService.getSpendingInsights(username);


                default:
                    return Map.of("error", "Unknown tool: " + name);
            }
        } catch (Exception e) {
            log.error("Error executing local tool call: {}", name, e);
            return Map.of("error", "Failed to retrieve information: " + e.getMessage());
        }
    }

    private String extractTextFromChunk(String json) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
            List<?> candidates = (List<?>) parsed.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                if (content != null) {
                    List<?> parts = (List<?>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        if (part.containsKey("text")) {
                            return (String) part.get("text");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Map<?, ?> extractFunctionCallFromChunk(String json) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
            List<?> candidates = (List<?>) parsed.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                if (content != null) {
                    List<?> parts = (List<?>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        if (part.containsKey("functionCall")) {
                            return (Map<?, ?>) part.get("functionCall");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

