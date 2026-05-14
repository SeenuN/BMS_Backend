package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.service.ChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
