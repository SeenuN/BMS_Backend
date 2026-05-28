package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.service.ChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> sendMessage(@RequestBody Map<String, Object> body) {
        try {
            String userMessage = (String) body.get("message");
            if (userMessage == null || userMessage.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("reply", "Please enter a message."));
            }

            // Conversation history: [{role: "user"|"model", text: "..."}]
            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());

            String reply = chatbotService.chat(userMessage, history);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            log.error("Chatbot error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("reply", "Error: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(90_000L); // 90s timeout

        String userMessage = (String) body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            try {
                emitter.send(SseEmitter.event().data("Please enter a message."));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());

        // Capture security context authentication from request thread
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Process chatbot response stream asynchronously, propagating security context
        CompletableFuture.runAsync(() -> {
            if (auth != null) {
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            try {
                chatbotService.chatStream(userMessage, history, emitter);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }
}


