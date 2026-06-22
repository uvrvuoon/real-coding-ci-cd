package com.example.db2026termproject.controller;

import com.example.db2026termproject.entity.ChatRoom;
import com.example.db2026termproject.entity.Message;
import com.example.db2026termproject.repository.ChatRoomRepository;
import com.example.db2026termproject.repository.MessageRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    /**
     * ChatRoom.cno 또는 receiveCno 필드가 가끔 Customer 프록시 객체로 로딩될 수 있다.
     * 이 헬퍼는 String이면 그대로, 객체이면 리플렉션으로 getCno()를 호출해 ID 문자열을 추출한다.
     */
    private String extractCnoStr(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return ((String) obj).trim();
        try {
            java.lang.reflect.Method method = obj.getClass().getMethod("getCno");
            Object result = method.invoke(obj);
            if (result != null) return String.valueOf(result).trim();
        } catch (Exception e) {
            String str = obj.toString();
            if (str.contains("cno=")) {
                int start = str.indexOf("cno=") + 4;
                int end = str.indexOf(",", start);
                if (end == -1) end = str.indexOf(")", start);
                if (end != -1) return str.substring(start, end).trim();
            }
        }
        return obj.toString().trim();
    }

    /**
     * [단계 5] 채팅방 생성 또는 기존 방 반환 API.
     * 같은 판매자-구매자 쌍의 채팅방이 이미 있으면 기존 roomNo를 반환하고,
     * 없으면 새 채팅방을 생성한다 (roomNo는 Oracle IDENTITY가 자동 채번).
     */
    @PostMapping("/room")
    public ResponseEntity<?> createOrEnterRoom(
            @RequestParam Integer itemNo,
            @RequestParam String sellerCno,
            HttpSession session) {

        String buyerCno = (String) session.getAttribute("loggedInUser");
        if (buyerCno == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        // 동일 판매자-구매자 쌍의 채팅방이 이미 존재하면 첫 번째(가장 오래된) 방을 반환한다
        List<ChatRoom> existingRooms = chatRoomRepository.findByCnoAndReceiveCno(sellerCno, buyerCno);
        if (!existingRooms.isEmpty()) {
            return ResponseEntity.ok(existingRooms.get(0).getRoomNo());
        }

        ChatRoom newRoom = new ChatRoom();
        newRoom.setReceiveCno(buyerCno);
        newRoom.setCno(sellerCno);
        newRoom.setItemNo(itemNo);
        newRoom.setCreateDateTime(LocalDateTime.now());

        // save() 후 Oracle이 생성한 roomNo가 newRoom 객체에 자동으로 채워진다
        chatRoomRepository.save(newRoom);
        return ResponseEntity.ok(newRoom.getRoomNo());
    }

    /**
     * [단계 5] 내 채팅방 목록 조회 API.
     * 각 채팅방에 대해 상대방 ID, 마지막 메시지, 안 읽은 메시지 수를 함께 반환한다.
     */
    @GetMapping("/list")
    public ResponseEntity<?> getMyRooms(HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUser");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        List<ChatRoom> rooms = chatRoomRepository.findAllByUser(userId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (ChatRoom room : rooms) {
            Map<String, Object> map = new HashMap<>();
            map.put("roomNo", room.getRoomNo());
            map.put("itemNo", room.getItemNo());

            // 내가 판매자(cno)이면 상대방은 구매자(receiveCno), 그 반대도 마찬가지
            String roomCno = extractCnoStr(room.getCno());
            String roomReceiveCno = extractCnoStr(room.getReceiveCno());
            String currentUserId = userId.trim();
            String partnerCno = roomCno.equalsIgnoreCase(currentUserId) ? roomReceiveCno : roomCno;
            map.put("partnerCno", partnerCno);

            Long unreadCount = messageRepository.countUnreadMessages(room.getRoomNo(), userId);
            map.put("unreadCount", unreadCount);

            List<Message> messages = messageRepository.findByRoomNoOrderBySentDateTimeAsc(room.getRoomNo());
            if (!messages.isEmpty()) {
                Message lastMsg = messages.get(messages.size() - 1);
                map.put("lastMessage", lastMsg.getContent());
                map.put("lastMessageTime", lastMsg.getSentDateTime());
            } else {
                map.put("lastMessage", "대화 기록이 없습니다.");
                map.put("lastMessageTime", room.getCreateDateTime());
            }

            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * [단계 5] 메시지 전송 API.
     * sender는 실제 회원 ID 대신 "S"(판매자) 또는 "B"(구매자) 역할 코드로 저장한다.
     * 이는 DB에서 발신자 역할을 고정된 코드로 관리하기 위함이다.
     * 화면에 표시할 때는 getMessages에서 실제 ID로 변환한다.
     */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestParam Integer roomNo, @RequestParam String content, HttpSession session) {
        String senderId = (String) session.getAttribute("loggedInUser");
        if (senderId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        ChatRoom room = chatRoomRepository.findById(roomNo).orElse(null);
        if (room == null) return ResponseEntity.badRequest().body("존재하지 않는 채팅방입니다.");

        Message msg = new Message();
        msg.setRoomNo(roomNo);
        msg.setContent(content);
        msg.setSentDateTime(LocalDateTime.now());
        msg.setIsRead("N");

        System.out.println("디버깅용 내용: " + msg.getContent());
        System.out.println("디버깅용 발신자: " + msg.getSender());

        // 발신자가 판매자이면 "S", 구매자이면 "B"로 저장
        String sellerId = extractCnoStr(room.getCno());
        if (senderId.trim().equalsIgnoreCase(sellerId)) {
            msg.setSender("S");
        } else {
            msg.setSender("B");
        }

        System.out.println("DEBUG [데이터 전송 확인]");
        System.out.println("RoomNo: " + msg.getRoomNo());
        System.out.println("Content: " + msg.getContent());
        System.out.println("IsRead: " + msg.getIsRead());
        System.out.println("Sender: " + msg.getSender());
        System.out.println("SentDateTime: " + msg.getSentDateTime());

        messageRepository.save(msg);
        return ResponseEntity.ok("메시지가 전송되었습니다.");
    }

    /**
     * [단계 5] 채팅방 메시지 목록 조회 및 읽음 처리 API.
     *
     * DB에는 sender가 "S"/"B" 역할 코드로 저장되어 있다.
     * 이 API에서 판매자(cno) → "S", 구매자(receiveCno) → "B"의 역방향 매핑을 수행해
     * 프론트엔드에 실제 회원 ID를 전달한다.
     *
     * 읽음 처리: 상대방이 보낸 메시지(내 ID와 다른 sender)의 isRead를 "Y"로 갱신한다.
     * @Transactional이 있어야 더티 체킹으로 자동 UPDATE된다.
     */
    @Transactional
    @GetMapping("/messages/{roomNo}")
    public ResponseEntity<?> getMessages(@PathVariable Integer roomNo, HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUser");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        ChatRoom room = chatRoomRepository.findById(roomNo).orElse(null);
        List<Message> messages = messageRepository.findByRoomNoOrderBySentDateTimeAsc(roomNo);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Message m : messages) {
            Map<String, Object> map = new HashMap<>();
            map.put("roomNo", m.getRoomNo());
            map.put("seqNo", m.getSeqNo());
            map.put("content", m.getContent());
            map.put("sentDateTime", m.getSentDateTime());
            map.put("isRead", m.getIsRead());

            // DB의 "S"/"B"를 실제 회원 ID로 역변환한다
            String rawSender = m.getSender() != null ? m.getSender().trim() : "";
            String convertedSender = rawSender;

            if (room != null) {
                String sellerId = extractCnoStr(room.getCno());
                String buyerId = extractCnoStr(room.getReceiveCno());

                if ("b".equalsIgnoreCase(rawSender)) {
                    convertedSender = buyerId;
                } else if ("s".equalsIgnoreCase(rawSender)) {
                    convertedSender = sellerId;
                }
            }
            map.put("sender", convertedSender);
            response.add(map);

            // 내가 받은 메시지(상대방이 보낸 것)만 읽음 처리한다
            if ("N".equals(m.getIsRead()) && !convertedSender.equalsIgnoreCase(userId)) {
                m.setIsRead("Y");
                messageRepository.save(m);
            }
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUser");
        if (userId == null) return ResponseEntity.status(401).body("");
        return ResponseEntity.ok(userId.trim());
    }
}