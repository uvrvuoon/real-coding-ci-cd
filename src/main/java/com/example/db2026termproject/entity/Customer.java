package com.example.db2026termproject.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Customer")
public class Customer {

    @Id
    @Column(name = "cno", length = 20)
    private String cno;

    @Column(name = "passwd", length = 20, nullable = false)
    private String passwd;

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "region", length = 50)
    private String region;
}