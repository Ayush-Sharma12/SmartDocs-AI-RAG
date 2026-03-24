package com.ayush.docsai.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnswerResponse {
    String question;
    String answer;
    Long documentId;
}
