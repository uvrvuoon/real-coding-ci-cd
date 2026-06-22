package com.example.db2026termproject.controller;

import com.example.db2026termproject.entity.Item;
import com.example.db2026termproject.entity.ItemId;
import com.example.db2026termproject.entity.PurchaseReq;
import com.example.db2026termproject.repository.ItemRepository;
import com.example.db2026termproject.repository.PurchaseReqRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class PurchaseReqController {

    @Autowired
    private PurchaseReqRepository purchaseReqRepository;

    @Autowired
    private ItemRepository itemRepository;

    // 1. 구매 요청 제출 API (구매자 B, C가 사용)
    @PostMapping("/submit")
    public ResponseEntity<?> submitRequest(
            @RequestParam String sellerCno, // 판매자 회원번호
            @RequestParam Integer itemNo,   // 물품 번호
            @RequestParam Integer reqPrice, // 제시할 희망 가격
            @RequestParam(required = false) String reqMessage, // 함께 보낼 메시지
            HttpSession session) {

        String buyerCno = (String) session.getAttribute("loggedInUser");
        if (buyerCno == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // ★ 평가 기준(예외 처리): 자기 자신의 물품에는 구매 요청 불가
        if (buyerCno.equals(sellerCno)) {
            return ResponseEntity.badRequest().body("자기 자신의 물품에는 구매 요청을 할 수 없습니다.");
        }

        // 해당 물품 정보 조회 (상태 확인용)
        ItemId id = new ItemId();
        id.setCno(sellerCno);
        id.setItemNo(itemNo);
        Item item = itemRepository.findById(id).orElse(null);

        if (item == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 물품입니다.");
        }

        // ★ 평가 기준(예외 처리): 이미 거래 완료/예약 중인 물품에 요청 방지
        if (!"판매 중".equals(item.getSellStatus().trim())) {
            return ResponseEntity.badRequest().body("이미 거래가 진행 중이거나 완료된 물품에는 요청할 수 없습니다.");
        }

        try {
            // 평가 기준: 요청 금액과 메시지 저장
            PurchaseReq req = new PurchaseReq();
            req.setRequestCno(buyerCno);
            req.setCno(sellerCno);
            req.setItemNo(itemNo);
            req.setReqDateTime(LocalDateTime.now());
            req.setReqPrice(reqPrice);
            req.setReqMessage(reqMessage);

            purchaseReqRepository.save(req);
            return ResponseEntity.ok("구매 요청이 성공적으로 전달되었습니다.");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 2. 특정 물품의 구매 요청 목록 조회 API (판매자 A가 사용)
    @GetMapping("/list")
    public ResponseEntity<?> getRequestList(
            @RequestParam Integer itemNo,
            HttpSession session) {

        String sellerCno = (String) session.getAttribute("loggedInUser");
        if (sellerCno == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // 평가 기준: 판매 자가 받은 여러 명의 구매 요청 목록 한 번에 확인
        List<PurchaseReq> requestList = purchaseReqRepository.findByCnoAndItemNo(sellerCno, itemNo);
        return ResponseEntity.ok(requestList);
    }
}