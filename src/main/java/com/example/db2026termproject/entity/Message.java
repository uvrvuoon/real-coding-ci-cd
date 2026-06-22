package com.example.db2026termproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "MESSAGE")
@IdClass(MessageId.class)
public class Message {

    @Id
    @Column(name = "ROOMNO")
    private Integer roomNo;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "SENDER", length = 20)
    private String sender;

    @Column(name = "SENTDATETIME")
    private LocalDateTime sentDateTime;

    @Column(name = "CONTENT", length = 1000)
    private String content;

    @Column(name = "ISREAD", length = 10)
    private String isRead;
}