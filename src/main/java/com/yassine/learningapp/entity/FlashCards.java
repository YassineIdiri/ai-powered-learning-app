package com.yassine.learningapp.entity;


import jakarta.persistence.*;

@Entity
@Table( name = "flash_cards")
public class FlashCards extends BaseEntity {
    private String title;
    private String content;
    private boolean isFavorite;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
}
