package com.ayush.docsai.controller;

import com.ayush.docsai.dto.DocumentResponse;
import com.ayush.docsai.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> getDocuments(Authentication authentication) {
        return documentService.getUserDocuments((Long) authentication.getDetails());
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file,
                                                           Authentication authentication) throws IOException {
        DocumentResponse response = documentService.uploadDocument((Long) authentication.getDetails(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{documentId}")
    public Map<String, String> deleteDocument(@PathVariable Long documentId, Authentication authentication)
            throws IOException {
        documentService.deleteDocument((Long) authentication.getDetails(), documentId);
        return Map.of("message", "Document deleted successfully");
    }
}
