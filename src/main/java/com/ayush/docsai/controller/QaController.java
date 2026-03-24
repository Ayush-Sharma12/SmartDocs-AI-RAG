package com.ayush.docsai.controller;

import com.ayush.docsai.dto.AnswerResponse;
import com.ayush.docsai.dto.QuestionRequest;
import com.ayush.docsai.service.QaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qa")
@CrossOrigin
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    public AnswerResponse askQuestion(@Valid @RequestBody QuestionRequest request, Authentication authentication) {
        return qaService.answerQuestion((Long) authentication.getDetails(), request);
    }
}
