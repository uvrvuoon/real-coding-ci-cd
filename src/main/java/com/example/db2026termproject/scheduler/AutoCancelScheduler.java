package com.example.db2026termproject.scheduler;

import com.example.db2026termproject.entity.Item;
import com.example.db2026termproject.repository.ItemRepository;
import com.example.db2026termproject.repository.PurchaseReqRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 자동 취소 스케줄러.
 * 5초마다 실행되어 resDateTime이 48시간을 초과한 '예약 중' 물품을 '판매 중'으로 되돌린다.
 *
 * 처리 순서:
 *   1. 만료된 물품 목록 조회 (구매 요청 삭제 시 PK가 필요하므로 먼저 조회)
 *   2. 각 물품의 구매 요청 전체 삭제
 *   3. 벌크 UPDATE로 sellStatus='판매 중', resDateTime=null 일괄 갱신
 *
 * 주의: @Transactional과 @Scheduled를 함께 쓰려면 메서드가 public이어야 한다.
 * deleteAllRequestsByItem은 @jakarta.transaction.Transactional을 사용하는데,
 * 이 메서드를 외부 @Transactional 범위 안에서 호출하면 동일 트랜잭션에 참여한다.
 */
@Component
public class AutoCancelScheduler {

    @Autowired private ItemRepository itemRepository;
    @Autowired private PurchaseReqRepository purchaseReqRepository;

    @Transactional
    @Scheduled(fixedRate = 5000)
    public void cancelExpiredReservations() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(48);

        // 1단계: 만료된 물품 목록을 먼저 확보한다.
        //        이후 벌크 UPDATE로 상태가 바뀌면 조회할 수 없으므로 순서가 중요하다.
        List<Item> expiredItems = itemRepository.findExpiredReservations(thresholdTime);

        if (expiredItems.isEmpty()) return;

        // 2단계: 각 물품에 달린 구매 요청을 모두 삭제한다.
        for (Item item : expiredItems) {
            purchaseReqRepository.deleteAllRequestsByItem(item.getCno(), item.getItemNo());
        }

        // 3단계: 엔티티 load → save 방식 대신 벌크 UPDATE를 사용한다.
        //        load → save 방식은 건당 UPDATE가 발생하고, Jakarta/Spring 트랜잭션
        //        어노테이션 혼용 시 롤백될 위험이 있다.
        int count = itemRepository.resetExpiredReservations(thresholdTime);

        System.out.println("⚠️ [시스템] 예약 48시간 초과로 " + count + "개 물품이 '판매 중'으로 복귀 처리됨");
    }
}