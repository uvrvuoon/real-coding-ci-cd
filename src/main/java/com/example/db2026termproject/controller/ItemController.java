package com.example.db2026termproject.controller;

import com.example.db2026termproject.entity.Item;
import com.example.db2026termproject.entity.ItemId;
import com.example.db2026termproject.repository.ItemRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    // ★ 프론트엔드(웹 브라우저)에서 바로 읽을 수 있도록 static 하위 폴더로 경로 수정!
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    @PostMapping("/register")
    public ResponseEntity<?> registerItem(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String category,
            @RequestParam Integer price,
            @RequestParam(required = false) String tradePlace,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos, // ★ 프론트엔드와 변수명 통일 (images -> photos)
            HttpSession session) {

        // 1. 로그인 상태 확인 (예외 처리)
        String cno = (String) session.getAttribute("loggedInUser");
        if (cno == null) {
            return ResponseEntity.status(401).body("로그인이 필요한 서비스입니다. 로그인을 먼저 해주세요.");
        }

        // 2. 필수 입력값 검증 (회원님이 작성하신 완벽한 예외 처리 유지!)
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("물품 제목은 필수 입력 사항입니다.");
        }
        if (price == null || price < 0) {
            return ResponseEntity.badRequest().body("유효한 가격을 입력해 주세요.");
        }
        if (photos != null && photos.size() > 3) {
            return ResponseEntity.badRequest().body("이미지는 최대 3장까지만 등록할 수 있습니다.");
        }

        try {
            // 3. 새로운 물품 번호(itemNo) 채번 (COALESCE 덕분에 바로 +1 가능)
            Integer nextItemNo = itemRepository.findMaxItemNoByCno(cno) + 1;

            // 4. 물품 엔티티 생성 및 데이터 세팅
            Item item = new Item();
            item.setCno(cno);
            item.setItemNo(nextItemNo);
            item.setTitle(title);
            item.setDescription(description);
            item.setCategory(category);
            item.setPrice(price);
            item.setTradePlace(tradePlace);
            item.setRegDateTime(LocalDateTime.now()); // 현재 시간 자동 저장
            item.setSellStatus("판매 중"); // 초기 상태 고정

            // 5. 다중 이미지 업로드 처리
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs(); // 폴더가 없으면 생성

            if (photos != null && !photos.isEmpty()) {
                for (int i = 0; i < photos.size(); i++) {
                    MultipartFile file = photos.get(i);
                    if (!file.isEmpty()) {
                        // 파일명 중복 방지를 위해 현재 시간 밀리초를 파일명 앞에 붙임
                        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                        File dest = new File(UPLOAD_DIR + fileName);
                        file.transferTo(dest); // 실제 서버 폴더에 파일 저장

                        // DB 저장 컬럼명을 photo1, photo2, photo3에 맞게 매핑
                        if (i == 0) item.setPhoto1(fileName);
                        else if (i == 1) item.setPhoto2(fileName);
                        else if (i == 2) item.setPhoto3(fileName);
                    }
                }
            }

            // 6. DB에 물품 정보 저장
            itemRepository.save(item);

            return ResponseEntity.ok("물품이 성공적으로 등록되었습니다!");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ★ 추가된 물품 상세 조회 API
    @GetMapping("/detail")
    public ResponseEntity<?> getItemDetail(
            @RequestParam("cno") String cno,
            @RequestParam("itemNo") Integer itemNo) {

        // ItemId 복합키 객체를 사용하여 단건 조회
        Optional<Item> itemOptional = itemRepository.findById(new ItemId(cno, itemNo));

        if (itemOptional.isPresent()) {
            return ResponseEntity.ok(itemOptional.get());
        } else {
            return ResponseEntity.status(404).body("물품을 찾을 수 없습니다.");
        }
    }

    // 마이페이지: 내 물품 목록 불러오기 API
    @GetMapping("/my")
    public ResponseEntity<?> getMyItems(HttpSession session) {
        String cno = (String) session.getAttribute("loggedInUser");
        if (cno == null) return ResponseEntity.status(401).body("로그인 필요");

        // 엔티티가 아닌 DTO로 100% 안전하게 조회!
        List<com.example.db2026termproject.dto.ItemDTO> myItems = itemRepository.findMyItemsByCno(cno);
        return ResponseEntity.ok(myItems);
    }

    // ★ 구매 확정(거래 완료) API 추가
    @Transactional // 더티 체킹을 통한 자동 UPDATE 쿼리 발생을 위해 필수
    @PostMapping("/confirm-purchase")
    public ResponseEntity<?> confirmPurchase(
            @RequestParam("cno") String cno,
            @RequestParam("itemNo") Integer itemNo,
            @RequestParam("finalPrice") Integer finalPrice) {

        // 1. 복합키(ItemId)를 이용한 데이터 단건 조회
        Optional<Item> itemOptional = itemRepository.findById(new ItemId(cno, itemNo));

        if (!itemOptional.isPresent()) {
            return ResponseEntity.status(404).body("물품을 찾을 수 없습니다.");
        }

        // 2. 조회된 엔티티 값 변경
        Item item = itemOptional.get();
        item.setSellStatus("거래 완료");
        item.setFinalPrice(finalPrice);

        // @Transactional이 걸려 있으므로 메서드 종료 시점에 자동으로 DB에 Commit 됩니다.
        // itemRepository.save(item); // 명시적 호출도 문제 없음

        return ResponseEntity.ok("구매 확정 처리가 완료되었습니다.");
    }
}