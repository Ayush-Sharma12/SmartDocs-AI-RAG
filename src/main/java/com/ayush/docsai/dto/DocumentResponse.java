package com.ayush.docsai.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class DocumentResponse {
    Long id;
    String filename;
    String originalFilename;
    long fileSize;
    boolean indexed;
    Instant uploadedAt;
}
