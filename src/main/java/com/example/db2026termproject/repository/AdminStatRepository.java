package com.example.db2026termproject.repository;

import com.example.db2026termproject.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AdminStatRepository extends JpaRepository<Customer, String> {

    // 1. 그룹 함수(ROLLUP) 통계: 거래 지역 및 상품 카테고리별 등록 물품 수와 총 기대 수익 [7]
    @Query(value = "SELECT " +
                   "  CASE WHEN GROUPING(c.region) = 1 THEN '전체 총계' ELSE c.region END AS \"거래 지역\", " +
                   "  CASE " +
                   "    WHEN GROUPING(c.region) = 0 AND GROUPING(i.category) = 1 THEN '지역 소계' " +
                   "    WHEN GROUPING(c.region) = 1 AND GROUPING(i.category) = 1 THEN '-' " +
                   "    ELSE i.category " +
                   "  END AS \"상품 카테고리\", " +
                   "  COUNT(i.itemNo) AS \"등록 물품 수\", " +
                   "  SUM(i.price) AS \"총 기대 수익(원)\" " +
                   "FROM Customer c " +
                   "JOIN Item i ON c.cno = i.cno " +
                   "GROUP BY ROLLUP(c.region, i.category) " +
                   "ORDER BY c.region, i.category", nativeQuery = true)
    List<Map<String, Object>> getGroupStat();

    // 2. 윈도우 함수(RANK) 통계: 지역별 판매자 수익 순위 [8]
    @Query(value = "SELECT " +
                   "  c.region AS \"거래 지역\", " +
                   "  c.nickname AS \"판매자 닉네임\", " +
                   "  COUNT(i.itemNo) AS \"등록 물품 수\", " +
                   "  SUM(i.price) AS \"총 기대 수익(원)\", " +
                   "  RANK() OVER (PARTITION BY c.region ORDER BY SUM(i.price) DESC) AS \"지역별_수익_순위\" " +
                   "FROM Customer c " +
                   "JOIN Item i ON c.cno = i.cno " +
                   "GROUP BY c.region, c.nickname " +
                   "ORDER BY c.region, \"지역별_수익_순위\"", nativeQuery = true)
    List<Map<String, Object>> getWindowStat();
}