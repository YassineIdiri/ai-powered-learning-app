package com.yassine.learningapp.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table( name = "documents")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Document extends BaseEntity {
    private String path;
    private String title;
    private Double fileSize;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
