package com.example.db2026termproject.entity;

import java.io.Serializable;
import lombok.Data;

@Data
public class PurchaseReqId implements Serializable {
    private String requestCno;
    private String cno;
    private Integer itemNo;
}