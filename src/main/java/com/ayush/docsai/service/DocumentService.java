package com.ayush.docsai.service;

import com.ayush.docsai.dto.DocumentResponse;
import com.ayush.docsai.entity.AppDocument;
import com.ayush.docsai.entity.User;
import com.ayush.docsai.repository.AppDocumentRepository;
import com.ayush.docsai.repository.UserRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final AppDocumentRepository appDocumentRepository;
    private final UserRepository userRepository;
    private final VectorStore vectorStore;
    private final Path uploadDir;

    public DocumentService(AppDocumentRepository appDocumentRepository,
                           UserRepository userRepository,
                           VectorStore vectorStore,
                           @Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.appDocumentRepository = appDocumentRepository;
        this.userRepository = userRepository;
        this.vectorStore = vectorStore;
        this.uploadDir = Path.of(uploadDir);
        Files.createDirectories(this.uploadDir);
    }

    @SuppressWarnings("null")
    public DocumentResponse uploadDocument(Long userId, MultipartFile file) throws IOException {
        User user = getUser(userId);
        validateFile(file);

        logger.info("Starting upload for userId={}, originalFilename={}", userId, file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        Path destination = uploadDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        logger.debug("Saved uploaded file to {}", destination);

        AppDocument document = appDocumentRepository.save(AppDocument.builder()
                .filename(storedFilename)
                .originalFilename(file.getOriginalFilename())
                .filePath(destination.toString())
                .fileSize(file.getSize())
                .indexed(false)
                .user(user)
                .build());

        indexDocument(user.getId(), document);
        document.setIndexed(true);
        logger.info("Indexed document id={} for userId={}", document.getId(), userId);
        return toResponse(appDocumentRepository.save(document));
    }

    public List<DocumentResponse> getUserDocuments(Long userId) {
        User user = getUser(userId);
        return appDocumentRepository.findByUserOrderByUploadedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @SuppressWarnings("null")
    public void deleteDocument(Long userId, Long documentId) throws IOException {
        AppDocument document = getOwnedDocument(userId, documentId);
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.eq("userId", userId),
                builder.eq("documentId", documentId)
        ).build());

        boolean deleted = Files.deleteIfExists(Path.of(document.getFilePath()));
        if (deleted) {
            logger.debug("Deleted file {} for document id={}", document.getFilePath(), documentId);
        } else {
            logger.warn("File {} did not exist when deleting document id={}", document.getFilePath(), documentId);
        }
        appDocumentRepository.delete(document);
        logger.info("Removed document id={} for userId={}", documentId, userId);
    }

    public AppDocument getOwnedDocument(Long userId, Long documentId) {
        User user = getUser(userId);
        return appDocumentRepository.findByIdAndUser(documentId, user)
                .orElseThrow(() -> {
                    logger.debug("Document id={} not found for userId={}", documentId, userId);
                    return new IllegalArgumentException("Document not found: id=" + documentId);
                });
    }

    @SuppressWarnings("null")
    private void indexDocument(Long userId, AppDocument appDocument) {
        var config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                        .withNumberOfBottomTextLinesToDelete(0)
                        .withNumberOfTopPagesToSkipBeforeDelete(0)
                        .build())
                .withPagesPerDocument(1)
                .build();

        var pdfReader = new PagePdfDocumentReader(new FileSystemResource(appDocument.getFilePath()), config);
        var textSplitter = new TokenTextSplitter();

        List<Document> chunks = textSplitter.apply(pdfReader.get()).stream()
                .map(chunk -> {
                    chunk.getMetadata().put("userId", userId);
                    chunk.getMetadata().put("documentId", appDocument.getId());
                    chunk.getMetadata().put("filename", appDocument.getOriginalFilename());
                    return chunk;
                })
                .toList();

        vectorStore.accept(chunks);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a PDF file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be 10MB or less");
        }
        String originalFilename = file.getOriginalFilename();
        String filename = originalFilename == null ? "" : originalFilename.toLowerCase();
        if (!filename.endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
    }

    @SuppressWarnings("null")
    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
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

    private String sanitizeFilename(String filename) {
        if (filename == null) return "";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
