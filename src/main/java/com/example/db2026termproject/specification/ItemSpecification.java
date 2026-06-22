package com.example.db2026termproject.specification;

import com.example.db2026termproject.entity.Item;
import org.springframework.data.jpa.domain.Specification;

public class ItemSpecification {

    // 1. 제목 검색 조건 (isNot이 true면 NOT LIKE 수행)
    public static Specification<Item> searchTitle(String title, boolean isNot) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) return null;
            if (isNot) return cb.notLike(root.get("title"), "%" + title + "%");
            return cb.like(root.get("title"), "%" + title + "%");
        };
    }

    // 2. 카테고리 검색 조건 (isNot이 true면 NOT EQUAL 수행)
    public static Specification<Item> searchCategory(String category, boolean isNot) {
        return (root, query, cb) -> {
            if (category == null || category.trim().isEmpty()) return null;
            if (isNot) return cb.notEqual(root.get("category"), category);
            return cb.equal(root.get("category"), category);
        };
    }

    // 3. 가격 범위 검색 조건 (isNot이 true면 NOT BETWEEN 수행)
    public static Specification<Item> searchPrice(Integer minPrice, Integer maxPrice, boolean isNot) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            // 값이 안 들어왔을 경우 기본 최솟값/최댓값 설정
            int min = (minPrice != null) ? minPrice : 0;
            int max = (maxPrice != null) ? maxPrice : Integer.MAX_VALUE;
            
            if (isNot) return cb.not(cb.between(root.get("price"), min, max));
            return cb.between(root.get("price"), min, max);
        };
    }
}