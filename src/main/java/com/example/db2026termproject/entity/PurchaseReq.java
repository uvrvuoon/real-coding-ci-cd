package com.example.db2026termproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "PurchaseReq")
@IdClass(PurchaseReqId.class)
public class PurchaseReq {

    @Id
    @Column(name = "requestCno", length = 20)
    private String requestCno;

    @Id
    @Column(name = "cno", length = 20)
    private String cno;

    @Id
    @Column(name = "itemNo")
    private Integer itemNo;

    @Column(name = "reqDateTime")
    private LocalDateTime reqDateTime;

    @Column(name = "reqPrice")
    private Integer reqPrice;

    @Column(name = "reqMessage", length = 500)
    private String reqMessage;
}