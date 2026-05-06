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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin
public class DocumentController {

    private final DocumentService documentService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> getDocuments(Authentication authentication) {
        Long userId = (Long) authentication.getDetails();
        logger.info("Fetching documents for userId={}", userId);
        return documentService.getUserDocuments(userId);
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file,
                                                           Authentication authentication) throws IOException {
        Long userId = (Long) authentication.getDetails();
        logger.info("Uploading document for userId={}, originalFilename={}", userId, file.getOriginalFilename());
        DocumentResponse response = documentService.uploadDocument(userId, file);
        logger.info("Uploaded document id={} for userId={}", response.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{documentId}")
    public Map<String, String> deleteDocument(@PathVariable Long documentId, Authentication authentication)
            throws IOException {
        Long userId = (Long) authentication.getDetails();
        logger.info("Deleting document id={} for userId={}", documentId, userId);
        documentService.deleteDocument(userId, documentId);
        logger.info("Deleted document id={} for userId={}", documentId, userId);
        return Map.of("message", "Document deleted successfully");
    }
}
