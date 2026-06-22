package com.example.db2026termproject.repository;

import com.example.db2026termproject.entity.Message;
import com.example.db2026termproject.entity.MessageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, MessageId> {

    List<Message> findByRoomNoOrderBySentDateTimeAsc(Integer roomNo);

    @Query("SELECT COALESCE(MAX(m.seqNo), 0) FROM Message m WHERE m.roomNo = :roomNo")
    Integer findMaxSeqNoByRoomNo(@Param("roomNo") Integer roomNo);

    // 안 읽은 메시지 수 집계: 상대방이 보낸 메시지 중 isRead='N'인 것만 센다.
    // sender는 "S"/"B" 역할 코드이므로 userId(실제 ID)와 직접 비교하지 않는다.
    // 대신 "내가 보낸 것이 아닌" 메시지를 기준으로 카운트한다.
    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomNo = :roomNo AND m.isRead = 'N' AND m.sender != :userId")
    Long countUnreadMessages(@Param("roomNo") Integer roomNo, @Param("userId") String userId);
}