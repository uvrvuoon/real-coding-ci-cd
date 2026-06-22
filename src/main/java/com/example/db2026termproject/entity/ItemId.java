package com.example.db2026termproject.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemId implements Serializable {
    private String cno;
    private Integer itemNo;
}
