package com.Nexus.Chatter.controller;

import com.Nexus.Chatter.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/{botId}/upload")
    public ResponseEntity<String> uploadDocument(
            @PathVariable UUID botId,
            @RequestParam("file") MultipartFile file) {
        try {
            String result = documentService.ingestFile(botId, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}