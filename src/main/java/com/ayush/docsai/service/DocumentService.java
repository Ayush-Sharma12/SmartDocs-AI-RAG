package com.ayush.docsai.service;

import com.ayush.docsai.dto.DocumentResponse;
import com.ayush.docsai.entity.AppDocument;
import com.ayush.docsai.entity.User;
import com.ayush.docsai.repository.AppDocumentRepository;
import com.ayush.docsai.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String PDF_EXTENSION = ".pdf";

    private final AppDocumentRepository appDocumentRepository;
    private final UserRepository userRepository;
    private final VectorStore vectorStore;
    private final Path uploadDir;

    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    public DocumentService(
            AppDocumentRepository appDocumentRepository,
            UserRepository userRepository,
            VectorStore vectorStore,
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) throws IOException {

        this.appDocumentRepository = appDocumentRepository;
        this.userRepository = userRepository;
        this.vectorStore = vectorStore;
        this.uploadDir = Path.of(uploadDir);

        Files.createDirectories(this.uploadDir);
    }

    public DocumentResponse uploadDocument(Long userId, MultipartFile file) throws IOException {

        validateFile(file);

        User user = getUser(userId);

        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateStoredFilename(originalFilename);

        Path destination = uploadDir.resolve(storedFilename);

        logger.info("Uploading document for userId={}, filename={}", userId, originalFilename);

        Files.copy(
                file.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        logger.debug("File stored at {}", destination);

        AppDocument savedDocument = appDocumentRepository.save(
                AppDocument.builder()
                        .filename(storedFilename)
                        .originalFilename(originalFilename)
                        .filePath(destination.toString())
                        .fileSize(file.getSize())
                        .indexed(false)
                        .user(user)
                        .build()
        );

        indexDocument(userId, savedDocument);

        savedDocument.setIndexed(true);

        AppDocument updatedDocument = appDocumentRepository.save(savedDocument);

        logger.info(
                "Document indexed successfully. documentId={}, userId={}",
                updatedDocument.getId(),
                userId
        );

        return toResponse(updatedDocument);
    }

    public List<DocumentResponse> getUserDocuments(Long userId) {

        User user = getUser(userId);

        return appDocumentRepository.findByUserOrderByUploadedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteDocument(Long userId, Long documentId) throws IOException {

        AppDocument document = getOwnedDocument(userId, documentId);

        deleteVectors(userId, documentId);

        Path filePath = Path.of(document.getFilePath());

        if (Files.deleteIfExists(filePath)) {
            logger.debug("Deleted file {}", filePath);
        } else {
            logger.warn("File not found while deleting: {}", filePath);
        }

        appDocumentRepository.delete(document);

        logger.info(
                "Document deleted successfully. documentId={}, userId={}",
                documentId,
                userId
        );
    }

    public AppDocument getOwnedDocument(Long userId, Long documentId) {

        User user = getUser(userId);

        return appDocumentRepository.findByIdAndUser(documentId, user)
                .orElseThrow(() -> {
                    logger.warn(
                            "Unauthorized or missing document access. userId={}, documentId={}",
                            userId,
                            documentId
                    );

                    return new IllegalArgumentException(
                            "Document not found: id=" + documentId
                    );
                });
    }

    private void indexDocument(Long userId, AppDocument appDocument) {

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(
                        new ExtractedTextFormatter.Builder()
                                .withNumberOfBottomTextLinesToDelete(0)
                                .withNumberOfTopPagesToSkipBeforeDelete(0)
                                .build()
                )
                .withPagesPerDocument(1)
                .build();

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                new FileSystemResource(appDocument.getFilePath()),
                config
        );

        List<Document> chunks = textSplitter.apply(pdfReader.get())
                .stream()
                .peek(chunk -> addMetadata(chunk, userId, appDocument))
                .toList();

        vectorStore.accept(chunks);

        logger.debug(
                "Indexed {} chunks for documentId={}",
                chunks.size(),
                appDocument.getId()
        );
    }

    private void addMetadata(Document chunk, Long userId, AppDocument appDocument) {

        var metadata = chunk.getMetadata();

        metadata.put("userId", userId);
        metadata.put("documentId", appDocument.getId());
        metadata.put("filename", appDocument.getOriginalFilename());
    }

    private void deleteVectors(Long userId, Long documentId) {

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        vectorStore.delete(
                builder.and(
                        builder.eq("userId", userId),
                        builder.eq("documentId", documentId)
                ).build()
        );
    }

    private void validateFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a PDF file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be 10MB or less");
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase().endsWith(PDF_EXTENSION)) {

            throw new IllegalArgumentException(
                    "Only PDF files are supported"
            );
        }
    }

    private User getUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found. userId={}", userId);
                    return new IllegalArgumentException("User not found");
                });
    }

    private DocumentResponse toResponse(AppDocument document) {

        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .indexed(document.isIndexed())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private String generateStoredFilename(String originalFilename) {

        return UUID.randomUUID() + "-"
                + sanitizeFilename(originalFilename);
    }

    private String sanitizeFilename(String filename) {

        if (filename == null || filename.isBlank()) {
            return "unknown.pdf";
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
