package com.example.db2026termproject.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [단계 9] 관리자 전용 통계 컨트롤러.
 * cno = "c0"인 관리자만 접근 가능하다.
 *
 * 두 가지 통계를 제공한다:
 *   1. ROLLUP - 지역별/카테고리별 물품 수와 기대 수익의 소계 및 전체 합계
 *   2. RANK   - 지역 내 판매자 기대 수익 순위 (윈도우 함수)
 */
@RestController
@RequestMapping("/api/admin")
public class AdminStatController {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * ROLLUP 통계.
     * GROUPING() 함수로 소계 행과 전체 총계 행을 구분해 레이블을 붙인다:
     *   - GROUPING(region)=0, GROUPING(category)=1 → 지역 소계 행
     *   - GROUPING(region)=1 → 전체 총계 행
     *
     * 결과 배열 인덱스: [0]=region, [1]=category, [2]=itemCount, [3]=expectedRevenue
     */
    @GetMapping("/stat/rollup")
    public ResponseEntity<?> getRollupStat(HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (!"c0".equals(loggedInUser)) return ResponseEntity.status(403).body("관리자만 접근 가능한 통계입니다.");

        String sql = "SELECT " +
                "CASE WHEN GROUPING(c.region) = 1 THEN '전체 총계' ELSE c.region END AS region, " +
                "CASE " +
                "    WHEN GROUPING(c.region) = 0 AND GROUPING(i.category) = 1 THEN '지역 소계' " +
                "    WHEN GROUPING(c.region) = 1 AND GROUPING(i.category) = 1 THEN '-' " +
                "    ELSE i.category " +
                "END AS category, " +
                "COUNT(i.itemNo) AS itemCount, " +
                "SUM(i.price) AS expectedRevenue " +
                "FROM Customer c " +
                "JOIN Item i ON c.cno = i.cno " +
                "GROUP BY ROLLUP(c.region, i.category) " +
                "ORDER BY c.region, i.category";

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("region", row[0]);
            map.put("category", row[1]);
            map.put("itemCount", row[2]);
            map.put("expectedRevenue", row[3]);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * RANK 통계.
     * PARTITION BY region으로 지역별로 독립적인 순위를 매긴다.
     * 동점자가 있으면 같은 순위를 부여하고 다음 순위를 건너뛴다 (표준 RANK 동작).
     *
     * 결과 배열 인덱스: [0]=region, [1]=nickname, [2]=itemCount, [3]=expectedRevenue, [4]=rank
     */
    @GetMapping("/stat/rank")
    public ResponseEntity<?> getRankStat(HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (!"c0".equals(loggedInUser)) return ResponseEntity.status(403).body("관리자만 접근 가능한 통계입니다.");

        String sql = "SELECT " +
                "c.region AS region, " +
                "c.nickname AS nickname, " +
                "COUNT(i.itemNo) AS itemCount, " +
                "SUM(i.price) AS expectedRevenue, " +
                "RANK() OVER (PARTITION BY c.region ORDER BY SUM(i.price) DESC) AS rank_num " +
                "FROM Customer c " +
                "JOIN Item i ON c.cno = i.cno " +
                "GROUP BY c.region, c.nickname " +
                "ORDER BY c.region, rank_num";

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("region", row[0]);
            map.put("nickname", row[1]);
            map.put("itemCount", row[2]);
            map.put("expectedRevenue", row[3]);
            map.put("rank", row[4]);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}