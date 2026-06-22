package com.example.db2026termproject.controller;

import com.example.db2026termproject.dto.ItemDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [단계 3] 물품 복합 검색 컨트롤러.
 * Hibernate의 JPQL 대신 순수 Oracle SQL을 직접 작성한다.
 * 이유: JPQL로 동적 AND/OR/NOT 조건을 조합하면 Hibernate가 쿼리를 잘못 변환하는 경우가 있어
 * 네이티브 쿼리로 완전히 제어한다.
 */
@RestController
@RequestMapping("/api/items")
public class ItemSearchController {

    @PersistenceContext
    private EntityManager em;

    /**
     * 제목(NOT LIKE), 카테고리(NOT =), 가격 범위(NOT BETWEEN)를
     * AND/OR 연산자로 조합한 복합 검색을 수행한다.
     *
     * 조건 조합 방식:
     *   - operator1: 제목 조건과 카테고리 조건 사이의 논리 연산자 (AND/OR)
     *   - operator2: 카테고리 조건과 가격 조건 사이의 논리 연산자 (AND/OR)
     *   - notTitle/notCategory/notPrice: 각 조건을 부정(NOT)으로 전환
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchItems(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "false") boolean notTitle,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean notCategory,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "false") boolean notPrice,
            @RequestParam(defaultValue = "AND") String operator1,
            @RequestParam(defaultValue = "AND") String operator2,
            @RequestParam(defaultValue = "latest") String sortBy
    ) {
        String sql = "SELECT cno, itemNo, title, description, category, price, tradePlace, " +
                "regDateTime, resDateTime, sellStatus, photo1, photo2, photo3, finalPrice " +
                "FROM Item WHERE 1=1 ";

        if (title != null && !title.trim().isEmpty()) {
            sql += "AND " + (notTitle ? "title NOT LIKE :title " : "title LIKE :title ");
        }

        if (category != null && !category.trim().isEmpty()) {
            // operator1이 OR이면 앞 조건과 OR로 연결, 기본은 AND
            String op = "OR".equalsIgnoreCase(operator1) ? " OR " : " AND ";
            sql += op + (notCategory ? "category != :category " : "category = :category ");
        }

        if (minPrice != null || maxPrice != null) {
            String op = "OR".equalsIgnoreCase(operator2) ? " OR " : " AND ";
            if (notPrice) {
                // NOT 범위: 지정한 범위 밖의 가격만 조회
                if (minPrice != null && maxPrice != null) sql += op + "(price < :minPrice OR price > :maxPrice) ";
                else if (minPrice != null) sql += op + "price < :minPrice ";
                else if (maxPrice != null) sql += op + "price > :maxPrice ";
            } else {
                if (minPrice != null) sql += op + "price >= :minPrice ";
                if (maxPrice != null) sql += op + "price <= :maxPrice ";
            }
        }

        if ("priceAsc".equalsIgnoreCase(sortBy)) {
            sql += "ORDER BY price ASC";
        } else if ("priceDesc".equalsIgnoreCase(sortBy)) {
            sql += "ORDER BY price DESC";
        } else {
            sql += "ORDER BY regDateTime DESC";
        }

        Query query = em.createNativeQuery(sql);

        if (title != null && !title.trim().isEmpty()) query.setParameter("title", "%" + title + "%");
        if (category != null && !category.trim().isEmpty()) query.setParameter("category", category);
        if (minPrice != null) query.setParameter("minPrice", minPrice);
        if (maxPrice != null) query.setParameter("maxPrice", maxPrice);

        List<Object[]> rows = query.getResultList();
        List<ItemDTO> result = new ArrayList<>();

        // Oracle 네이티브 쿼리는 TIMESTAMP 컬럼을 java.sql.Timestamp로 반환한다.
        // ItemDTO 생성자는 LocalDateTime을 요구하므로 toLocalDateTime()으로 변환한다.
        for (Object[] row : rows) {
            result.add(new ItemDTO(
                    (String) row[0],
                    row[1] != null ? ((Number) row[1]).intValue() : null,
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    row[5] != null ? ((Number) row[5]).intValue() : null,
                    (String) row[6],
                    row[7] != null ? (LocalDateTime) row[7] : null,
                    row[8] != null ? (LocalDateTime) row[8] : null,
                    (String) row[9],
                    (String) row[10],
                    (String) row[11],
                    (String) row[12],
                    row[13] != null ? ((Number) row[13]).intValue() : null
            ));
        }

        return ResponseEntity.ok(result);
    }
}