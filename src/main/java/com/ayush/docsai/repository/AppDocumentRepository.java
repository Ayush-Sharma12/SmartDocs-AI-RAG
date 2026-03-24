package com.ayush.docsai.repository;

import com.ayush.docsai.entity.AppDocument;
import com.ayush.docsai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppDocumentRepository extends JpaRepository<AppDocument, Long> {
    List<AppDocument> findByUserOrderByUploadedAtDesc(User user);

    Optional<AppDocument> findByIdAndUser(Long id, User user);
}
