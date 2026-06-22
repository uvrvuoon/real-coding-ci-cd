package com.example.db2026termproject.entity;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Item")
@IdClass(ItemId.class)
public class Item {

    @Id
    @Column(name = "cno", length = 20)
    private String cno;

    @Id
    @Column(name = "itemNo")
    private Integer itemNo;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "price")
    private Integer price;

    @Column(name = "tradePlace", length = 200)
    private String tradePlace;

    @Column(name = "regDateTime")
    private LocalDateTime regDateTime;

    @Column(name = "resDateTime")
    private LocalDateTime resDateTime;

    @Column(name = "sellStatus", length = 20)
    private String sellStatus;

    @Column(name = "PHOTO1", length = 500)
    @JdbcType(VarcharJdbcType.class)
    private String photo1;

    @Column(name = "PHOTO2", length = 500)
    @JdbcType(VarcharJdbcType.class)
    private String photo2;

    @Column(name = "PHOTO3", length = 500)
    @JdbcType(VarcharJdbcType.class)
    private String photo3;

    @Column(name = "finalPrice")
    private Integer finalPrice;
}
