package com.example.db2026termproject.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 모든 필드를 채우는 생성자를 자동으로 만들어 줌.
public class ItemDTO {
    private String cno;
    private Integer itemNo;
    private String title;
    private String description;
    private String category;
    private Integer price;
    private String tradePlace;
    private LocalDateTime regDateTime;
    private LocalDateTime resDateTime;
    private String sellStatus;
    private String photo1;
    private String photo2;
    private String photo3;
    private Integer finalPrice;
}