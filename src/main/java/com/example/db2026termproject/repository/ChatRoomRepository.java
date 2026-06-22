package com.example.db2026termproject.repository;

import com.example.db2026termproject.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {

    // 동일한 판매자-구매자 쌍의 채팅방이 이미 존재하는지 확인한다.
    // itemNo는 무시하고 사람 기준으로만 중복을 체크해 채팅방이 1:1로 유지되도록 한다.
    @Query("SELECT c FROM ChatRoom c WHERE c.cno = :cno AND c.receiveCno = :receiveCno ORDER BY c.roomNo ASC")
    List<ChatRoom> findByCnoAndReceiveCno(@Param("cno") String cno, @Param("receiveCno") String receiveCno);

    // 내가 판매자(cno)이거나 구매자(receiveCno)인 모든 채팅방을 조회한다.
    @Query("SELECT c FROM ChatRoom c WHERE c.cno = :userId OR c.receiveCno = :userId")
    List<ChatRoom> findAllByUser(@Param("userId") String userId);
}