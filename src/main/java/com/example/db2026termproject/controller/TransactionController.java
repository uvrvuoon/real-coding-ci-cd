package com.example.db2026termproject.controller;

import com.example.db2026termproject.entity.Item;
import com.example.db2026termproject.entity.ItemId;
import com.example.db2026termproject.repository.ItemRepository;
import com.example.db2026termproject.repository.PurchaseReqRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PurchaseReqRepository purchaseReqRepository;

    /**
     * [단계 6] 구매 요청 승인 API.
     * 판매자가 특정 구매자의 요청을 승인하면:
     *   - 물품 상태 → '예약 중'
     *   - resDateTime 기록 (48시간 자동 취소 기준 시각)
     *   - 승인된 구매자 외 나머지 요청 자동 삭제
     *
     * sellStatus 비교 시 .trim() 필수: Oracle CHAR 타입은 공백 패딩이 붙어 반환된다.
     */
    @PostMapping("/approve")
    public ResponseEntity<?> approveRequest(
            @RequestParam Integer itemNo,
            @RequestParam String buyerCno,
            HttpSession session) {

        String sellerCno = (String) session.getAttribute("loggedInUser");
        if (sellerCno == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        ItemId id = new ItemId();
        id.setCno(sellerCno);
        id.setItemNo(itemNo);
        Item item = itemRepository.findById(id).orElse(null);

        if (item == null || !"판매 중".equals(item.getSellStatus().trim())) {
            return ResponseEntity.badRequest().body("승인할 수 없는 물품 상태입니다.");
        }

        item.setSellStatus("예약 중");
        item.setResDateTime(LocalDateTime.now()); // 스케줄러가 이 시각 기준으로 48시간을 계산한다
        itemRepository.save(item);

        // 승인된 구매자(buyerCno)를 제외한 나머지 요청을 일괄 삭제한다
        purchaseReqRepository.deleteOtherRequests(sellerCno, itemNo, buyerCno);

        return ResponseEntity.ok("성공적으로 거래가 승인되어 '예약 중'으로 변경되었습니다.");
    }

    /**
     * [단계 7] 거래 완료 처리 API.
     * '예약 중' 상태인 물품에 대해 판매자가 최종 거래 금액을 입력하고 거래를 종료한다.
     * 완료 후 물품 상태는 '거래 완료'로 변경된다.
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completeTransaction(
            @RequestParam Integer itemNo,
            @RequestParam Integer finalPrice,
            HttpSession session) {

        String sellerCno = (String) session.getAttribute("loggedInUser");
        if (sellerCno == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        ItemId id = new ItemId();
        id.setCno(sellerCno);
        id.setItemNo(itemNo);
        Item item = itemRepository.findById(id).orElse(null);

        if (item == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 물품입니다.");
        }

        if (!"예약 중".equals(item.getSellStatus().trim())) {
            return ResponseEntity.badRequest().body("예약 중인 물품만 거래 완료 처리할 수 있습니다.");
        }

        item.setSellStatus("거래 완료");
        item.setFinalPrice(finalPrice);
        itemRepository.save(item);

        return ResponseEntity.ok("거래가 성공적으로 완료되었습니다! 최종 금액: " + finalPrice + "원 저장 완료.");
    }
}