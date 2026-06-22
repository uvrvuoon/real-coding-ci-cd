package com.example.db2026termproject.repository;

import com.example.db2026termproject.entity.Item;
import com.example.db2026termproject.entity.ItemId;
import com.example.db2026termproject.dto.ItemDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, ItemId>, JpaSpecificationExecutor<Item> {

    @Query("SELECT new com.example.db2026termproject.dto.ItemDTO(" +
            "i.cno, i.itemNo, i.title, i.description, i.category, i.price, i.tradePlace, " +
            "i.regDateTime, i.resDateTime, i.sellStatus, i.photo1, i.photo2, i.photo3, i.finalPrice) " +
            "FROM Item i " +
            "WHERE (:title IS NULL OR i.title LIKE %:title%) " +
            "AND (:category IS NULL OR i.category = :category)")
    List<ItemDTO> searchByHand(@Param("title") String title, @Param("category") String category);

    // 새 물품 등록 시 itemNo 채번용. 판매자(cno)별로 독립적인 순번을 사용한다.
    @Query("SELECT COALESCE(MAX(i.itemNo), 0) FROM Item i WHERE i.cno = :cno")
    Integer findMaxItemNoByCno(@Param("cno") String cno);

    // Oracle sellStatus가 CHAR 타입이므로 TRIM을 적용해야 공백 패딩 문제를 피할 수 있다.
    @Query("SELECT i FROM Item i WHERE TRIM(i.sellStatus) = '예약 중' AND i.resDateTime < :thresholdTime")
    List<Item> findExpiredReservations(@Param("thresholdTime") LocalDateTime thresholdTime);

    // 스케줄러 3단계: 만료 물품 상태를 건당 save 없이 한 번의 UPDATE로 처리한다.
    // @Modifying은 SELECT가 아닌 DML 쿼리임을 JPA에 알려준다.
    @Modifying
    @Transactional
    @Query("UPDATE Item i SET i.sellStatus = '판매 중', i.resDateTime = null WHERE TRIM(i.sellStatus) = '예약 중' AND i.resDateTime < :thresholdTime")
    int resetExpiredReservations(@Param("thresholdTime") LocalDateTime thresholdTime);

    // 마이페이지: 엔티티 직접 반환 시 Hibernate가 CHAR 컬럼을 잘못 처리하는 경우가 있어 DTO로 반환한다.
    @Query("SELECT new com.example.db2026termproject.dto.ItemDTO(" +
            "i.cno, i.itemNo, i.title, i.description, i.category, i.price, i.tradePlace, " +
            "i.regDateTime, i.resDateTime, i.sellStatus, i.photo1, i.photo2, i.photo3, i.finalPrice) " +
            "FROM Item i " +
            "WHERE i.cno = :cno " +
            "ORDER BY i.regDateTime DESC")
    List<ItemDTO> findMyItemsByCno(@Param("cno") String cno);
}