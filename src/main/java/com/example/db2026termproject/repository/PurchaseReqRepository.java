package com.example.db2026termproject.repository;

import com.example.db2026termproject.entity.PurchaseReq;
import com.example.db2026termproject.entity.PurchaseReqId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseReqRepository extends JpaRepository<PurchaseReq, PurchaseReqId> {

    List<PurchaseReq> findByCnoAndItemNo(String cno, Integer itemNo);

    // 판매자가 특정 구매자를 승인했을 때, 나머지 구매자들의 요청을 일괄 삭제한다.
    @org.springframework.data.jpa.repository.Modifying
    @jakarta.transaction.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM PurchaseReq p WHERE p.cno = :sellerCno AND p.itemNo = :itemNo AND p.requestCno != :approvedBuyerCno")
    void deleteOtherRequests(@org.springframework.data.repository.query.Param("sellerCno") String sellerCno,
                             @org.springframework.data.repository.query.Param("itemNo") Integer itemNo,
                             @org.springframework.data.repository.query.Param("approvedBuyerCno") String approvedBuyerCno);

    // 48시간 초과로 예약이 자동 취소될 때, 해당 물품의 모든 구매 요청을 삭제한다.
    @org.springframework.data.jpa.repository.Modifying
    @jakarta.transaction.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM PurchaseReq p WHERE p.cno = :sellerCno AND p.itemNo = :itemNo")
    void deleteAllRequestsByItem(@org.springframework.data.repository.query.Param("sellerCno") String sellerCno,
                                 @org.springframework.data.repository.query.Param("itemNo") Integer itemNo);
}