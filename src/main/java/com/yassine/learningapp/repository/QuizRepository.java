package com.yassine.learningapp.repository;

import com.yassine.learningapp.entity.Quiz;
import com.yassine.learningapp.entity.QuizQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz,Long> {
    Page<Quiz> findByDocumentId(Long documentId, Pageable pageable);

//La est ce quon a besoind e la query et est ce que la querry ecrase le name
    @Query("SELECT q from Quiz q WHERE q.document.id = :documentId AND q.document.user.id = :userId")
    Page<Quiz> findByDocumentIdAndUserId(Long documentId, Long userId, Pageable pageable);

    long countByDocumentId(Long documentId);
}
