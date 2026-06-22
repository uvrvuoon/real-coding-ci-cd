package com.example.db2026termproject.entity;

import java.io.Serializable;
import lombok.Data;

@Data
public class MessageId implements Serializable {
    private Integer roomNo;
    private Integer seqNo;
}