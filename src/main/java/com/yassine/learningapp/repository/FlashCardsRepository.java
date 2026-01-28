package com.yassine.learningapp.repository;


import com.yassine.learningapp.entity.FlashCards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashCardsRepository extends JpaRepository<FlashCards, Long> {
    List<FlashCards> findByDocumentId(Long documentId);
}
