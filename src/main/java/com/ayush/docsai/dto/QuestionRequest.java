package com.ayush.docsai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionRequest {
    @NotBlank
    private String question;

    private Long documentId;
}
