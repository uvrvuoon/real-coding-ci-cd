package com.example.db2026termproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ChatRoom")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomNo")
    private Integer roomNo;

    @Column(name = "receiveCno", length = 20)
    private String receiveCno;

    @Column(name = "createDateTime")
    private LocalDateTime createDateTime;

    @Column(name = "cno", length = 20)
    private String cno;

    @Column(name = "itemNo")
    private Integer itemNo;
}