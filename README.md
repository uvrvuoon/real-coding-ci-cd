# 중고마켓 플랫폼

---

## 1. 프로젝트 개요

### 1.1 프로젝트 목적
Oracle XE 데이터베이스를 백엔드로 하는 중고 물품 거래 플랫폼 구현.  
회원 간 물품 등록, 구매 요청, 1:1 채팅, 거래 승인/완료, 예약 자동 취소, 관리자 통계 기능을 포함한다.

### 1.2 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.x |
| ORM | Spring Data JPA (Hibernate 6) |
| 데이터베이스 | Oracle XE 21c |
| 프론트엔드 | Vanilla HTML + CSS + JavaScript (fetch API) |
| 빌드 도구 | Gradle |

### 1.3 전체 아키텍처

```
브라우저 (HTML/JS)
        │  HTTP 요청 (fetch API)
        ▼
  Spring Boot (내장 Tomcat, port 8080)
        │
  ┌─────┴──────────────────────────┐
  │         Controller 계층         │
  │  AuthController                 │
  │  ItemController                 │
  │  ItemSearchController           │
  │  PurchaseReqController          │
  │  TransactionController          │
  │  ChatController                 │
  │  AdminStatController            │
  └─────┬──────────────────────────┘
        │  JPA / JPQL / Native SQL
  ┌─────┴──────────────────────────┐
  │        Repository 계층          │
  │  (Spring Data JPA Interface)    │
  └─────┬──────────────────────────┘
        │  JDBC (Oracle Driver)
        ▼
   Oracle XE 21c (localhost:1521/XE)
```

---

## 2. 패키지 구조

```
src/main/java/com/example/db2026termproject/
├── Db2026termprojectApplication.java   ← 앱 진입점, @EnableScheduling 선언
│
├── config/
│   └── WebMvcConfig.java               ← /uploads/** 정적 파일 경로 설정
│
├── entity/                             ← DB 테이블 매핑 클래스
│   ├── Customer.java
│   ├── Item.java / ItemId.java
│   ├── PurchaseReq.java / PurchaseReqId.java
│   ├── ChatRoom.java
│   └── Message.java / MessageId.java
│
├── dto/
│   └── ItemDTO.java                    ← 물품 조회 전용 전송 객체
│
├── repository/                         ← DB 접근 인터페이스 (Spring Data JPA)
│   ├── CustomerRepository.java
│   ├── ItemRepository.java
│   ├── PurchaseReqRepository.java
│   ├── ChatRoomRepository.java
│   ├── MessageRepository.java
│   └── AdminStatRepository.java
│
├── controller/                         ← REST API 엔드포인트
│   ├── AuthController.java
│   ├── ItemController.java
│   ├── ItemSearchController.java
│   ├── PurchaseReqController.java
│   ├── TransactionController.java
│   ├── ChatController.java
│   └── AdminStatController.java
│
├── scheduler/
│   └── AutoCancelScheduler.java        ← 48시간 예약 자동 취소
│
└── specification/
    └── ItemSpecification.java          ← JPA Criteria API 검색 조건 (미사용)

src/main/resources/static/             ← 프론트엔드 HTML
├── index.html          ← 로그인 화면
├── items.html          ← 물품 목록 + 복합 검색
├── register.html       ← 물품 등록
├── item_detail.html    ← 물품 상세 + 구매 요청
├── mypage.html         ← 내 물품 관리 (승인/거래완료)
├── chat_list.html      ← 채팅방 목록
├── chat.html           ← 1:1 채팅방
└── admin_dashboard.html ← 관리자 통계
```

---

## 3. 데이터베이스 테이블 구조

| 테이블 | 기본키 | 주요 컬럼 |
|--------|--------|----------|
| Customer | cno | passwd, nickname, phone, region |
| Item | (cno, itemNo) | title, price, sellStatus, resDateTime, photo1~3, finalPrice |
| PurchaseReq | (requestCno, cno, itemNo) | reqPrice, reqMessage, reqDateTime |
| ChatRoom | roomNo (IDENTITY) | cno(판매자), receiveCno(구매자), itemNo, createDateTime |
| MESSAGE | (roomNo, seqNo(IDENTITY)) | sender, content, sentDateTime, isRead |

> **복합키 테이블**: Item, PurchaseReq, MESSAGE는 JPA `@IdClass` 전략으로 복합키를 처리한다.  
> **IDENTITY 컬럼**: ChatRoom.roomNo, MESSAGE.seqNo는 Oracle IDENTITY 컬럼이며, INSERT 시 Oracle이 자동으로 값을 채번한다.

---

## 4. 계층별 소스코드 설명

### 4.1 Entity 계층

엔티티 클래스는 DB 테이블과 1:1로 매핑된다. Lombok `@Data`로 getter/setter/toString을 자동 생성하고, 복합키가 있는 테이블은 별도의 Id 클래스를 `@IdClass`로 연결한다.

**Entity 예시 — Item.java**

```java
@Data
@Entity
@Table(name = "Item")
@IdClass(ItemId.class)          // 복합키 클래스 연결
public class Item {

    @Id
    @Column(name = "cno", length = 20)
    private String cno;         // 판매자 회원번호 (복합키 1)

    @Id
    @Column(name = "itemNo")
    private Integer itemNo;     // 물품 번호 (복합키 2, 판매자별 독립 순번)

    @Column(name = "sellStatus", length = 20)
    private String sellStatus;  // "판매 중" / "예약 중" / "거래 완료"

    @Column(name = "resDateTime")
    private LocalDateTime resDateTime; // 예약 승인 시각, 48시간 자동 취소 기준

    @Column(name = "PHOTO1", length = 500)
    @JdbcType(VarcharJdbcType.class)   // Oracle CHAR 공백 패딩 문제 방지
    private String photo1;

    // ... 이하 생략
}
```

**나머지 Entity 요약**

| 클래스 | 특이사항 |
|--------|---------|
| Customer | 단순 단일키(cno). c0 = 관리자 계정 |
| ItemId | Item 복합키 클래스. `Serializable` 구현 필수 |
| PurchaseReq | 3개 컬럼 복합키. 같은 구매자가 같은 물품에 중복 요청 불가 |
| PurchaseReqId | PurchaseReq 복합키 클래스 |
| ChatRoom | roomNo에 `@GeneratedValue(IDENTITY)`. Oracle IDENTITY 컬럼이므로 수동으로 값을 세팅하면 ORA-32795 오류 발생 |
| Message | (roomNo, seqNo) 복합키. seqNo도 IDENTITY 자동 채번 |
| MessageId | Message 복합키 클래스 |

### 4.2 DTO

**ItemDTO.java**  
Item 엔티티를 그대로 반환하면 Hibernate가 CHAR 타입 컬럼을 잘못 처리하는 경우가 있어, 조회 전용 DTO로 변환해서 반환한다. `@AllArgsConstructor`로 JPQL `new` 생성자 표현식에서 바로 생성할 수 있다.

### 4.3 Repository 계층

Spring Data JPA `JpaRepository`를 상속받아 기본 CRUD를 자동으로 제공받는다. 커스텀 쿼리가 필요한 경우만 `@Query`로 별도 정의한다.

**CustomerRepository.java**
```java
Customer findByCnoAndPasswd(String cno, String passwd);
// 메서드명 규칙으로 자동 생성: WHERE cno = ? AND passwd = ?
```

**ItemRepository.java** — 주요 메서드

| 메서드 | 설명 |
|--------|------|
| `findMaxItemNoByCno(cno)` | 판매자별 itemNo 최댓값 조회. 새 물품 등록 시 +1해서 사용 |
| `findExpiredReservations(threshold)` | TRIM(sellStatus)='예약 중' AND resDateTime < threshold. TRIM은 Oracle CHAR 공백 패딩 대응 |
| `resetExpiredReservations(threshold)` | `@Modifying` 벌크 UPDATE. 엔티티 건당 save() 대신 단일 UPDATE로 처리 |
| `findMyItemsByCno(cno)` | 마이페이지용. DTO 직접 생성 JPQL 사용 |

**PurchaseReqRepository.java** — 주요 메서드

| 메서드 | 사용 시점 |
|--------|----------|
| `deleteOtherRequests(sellerCno, itemNo, approvedBuyerCno)` | 판매자가 특정 구매자를 승인할 때, 나머지 요청 일괄 삭제 |
| `deleteAllRequestsByItem(sellerCno, itemNo)` | 48시간 초과 자동 취소 시, 해당 물품의 모든 요청 삭제 |

**ChatRoomRepository.java** — 주요 메서드

```java
// 판매자-구매자 쌍 기준 중복 체크 (itemNo는 무시)
@Query("SELECT c FROM ChatRoom c WHERE c.cno = :cno AND c.receiveCno = :receiveCno ORDER BY c.roomNo ASC")
List<ChatRoom> findByCnoAndReceiveCno(String cno, String receiveCno);
```
> 동일한 판매자-구매자 사이에는 채팅방이 하나만 존재하도록 설계. itemNo가 달라도 같은 방을 재사용한다.

**MessageRepository.java** — 주요 메서드

```java
// 안 읽은 메시지 수 집계
// sender는 "S"/"B" 역할코드이므로 userId와 직접 비교할 수 없다.
// "내가 보낸 것이 아닌" 메시지 중 isRead='N'인 것을 센다.
@Query("SELECT COUNT(m) FROM Message m WHERE m.roomNo = :roomNo AND m.isRead = 'N' AND m.sender != :userId")
Long countUnreadMessages(Integer roomNo, String userId);
```

### 4.4 Controller 계층

**AuthController** (`/api/auth`)

| 엔드포인트 | 역할 |
|-----------|------|
| `POST /login` | cno+passwd로 인증. 성공 시 세션에 `loggedInUser` 저장. c0이면 ADMIN, 그 외 USER 역할 반환 |
| `POST /logout` | 세션 무효화 |

**ItemController** (`/api/items`)

| 엔드포인트 | 역할 |
|-----------|------|
| `POST /register` | 물품 등록. 사진 최대 3장 서버 파일시스템에 저장. itemNo는 판매자별 MAX+1로 채번 |
| `GET /detail` | 복합키(cno, itemNo)로 단건 조회 |
| `GET /my` | 세션의 판매자 기준 내 물품 목록 (DTO 반환) |
| `POST /confirm-purchase` | 구매 확정. 물품 상태 → '거래 완료', finalPrice 저장 |

**ItemSearchController** (`/api/items/search`)  
AND/OR/NOT 복합 조건 검색을 처리한다. JPQL 대신 순수 Oracle SQL을 동적으로 조립하는 방식을 채택했다. 이는 Hibernate가 복잡한 OR/NOT 조건을 포함한 동적 쿼리를 변환할 때 오작동하는 경우를 피하기 위함이다.

```java
// 조건 조합 예시
// title LIKE :title  [operator1]  category = :category  [operator2]  price <= :maxPrice
// 각 조건에 NOT 체크박스가 있어 NOT LIKE / != / 범위 초과로 전환 가능
```

**PurchaseReqController** (`/api/requests`)

| 엔드포인트 | 역할 |
|-----------|------|
| `POST /submit` | 구매 요청 제출. 자기 물품 요청 불가, '판매 중' 상태 아니면 거부 |
| `GET /list` | 판매자 기준 특정 물품의 구매 요청 목록 조회 |

**TransactionController** (`/api/transaction`)

| 엔드포인트 | 역할 |
|-----------|------|
| `POST /approve` | 구매 요청 승인. 상태 → '예약 중', resDateTime 기록, 나머지 요청 삭제 |
| `POST /complete` | 거래 완료. 상태 → '거래 완료', finalPrice 저장 |

> 두 메서드 모두 `item.getSellStatus().trim()`으로 비교한다. Oracle CHAR 타입은 고정 길이만큼 공백이 패딩되어 반환되므로 trim() 없이 비교하면 상태 체크가 항상 실패한다.

**ChatController** (`/api/chat`)

| 엔드포인트 | 역할 |
|-----------|------|
| `POST /room` | 채팅방 생성 또는 기존 방 반환. roomNo는 Oracle IDENTITY 자동 채번 |
| `GET /list` | 내가 참여한 채팅방 목록 + 마지막 메시지 + 안 읽은 메시지 수 |
| `POST /message` | 메시지 전송. sender를 "S"/"B" 역할코드로 DB에 저장 |
| `GET /messages/{roomNo}` | 메시지 목록 조회 + 읽음 처리. "S"/"B"를 실제 회원 ID로 역변환해서 반환 |
| `GET /me` | 현재 세션의 로그인 ID 반환 (프론트에서 내/상대 메시지 구분용) |

> **sender 역할코드 방식**: DB에는 "S"(판매자)/"B"(구매자)로 저장하고, API 응답 시 ChatRoom의 cno/receiveCno 필드와 대조해 실제 회원 ID로 변환한다. 이렇게 하면 회원 ID가 변경되어도 과거 메시지의 발신자 관계가 유지된다.

**AdminStatController** (`/api/admin`)

| 엔드포인트 | 역할 |
|-----------|------|
| `GET /stat/rollup` | 지역별/카테고리별 물품 수·기대 수익 (GROUP BY ROLLUP) |
| `GET /stat/rank` | 지역 내 판매자 기대 수익 순위 (RANK() OVER PARTITION BY) |

두 API 모두 c0(관리자) 세션만 허용하며, `EntityManager.createNativeQuery()`로 Oracle 전용 SQL을 직접 실행한다.

```sql
-- ROLLUP 예시: GROUPING() 함수로 소계/합계 행을 레이블로 구분
SELECT
  CASE WHEN GROUPING(c.region) = 1 THEN '전체 총계' ELSE c.region END,
  CASE WHEN GROUPING(i.category) = 1 THEN '지역 소계' ELSE i.category END,
  COUNT(i.itemNo), SUM(i.price)
FROM Customer c JOIN Item i ON c.cno = i.cno
GROUP BY ROLLUP(c.region, i.category)

-- RANK 예시: 지역별 독립 순위 산출
RANK() OVER (PARTITION BY c.region ORDER BY SUM(i.price) DESC)
```

### 4.5 Scheduler

**AutoCancelScheduler.java** — 5초마다 실행

```java
@Transactional
@Scheduled(fixedRate = 5000)
public void cancelExpiredReservations() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(48);

    // 1단계: 만료 물품 목록 조회 (구매 요청 삭제에 PK 필요)
    List<Item> expiredItems = itemRepository.findExpiredReservations(threshold);
    if (expiredItems.isEmpty()) return;

    // 2단계: 각 물품의 구매 요청 전체 삭제
    for (Item item : expiredItems)
        purchaseReqRepository.deleteAllRequestsByItem(item.getCno(), item.getItemNo());

    // 3단계: 벌크 UPDATE로 sellStatus 일괄 복구
    int count = itemRepository.resetExpiredReservations(threshold);
}
```

> 벌크 UPDATE를 선택한 이유: 엔티티를 개별 로드한 뒤 save()하는 방식은 건당 UPDATE가 발생하고, `@jakarta.transaction.Transactional`과 `@org.springframework.transaction.annotation.Transactional` 혼용 시 트랜잭션 충돌로 롤백될 위험이 있다. `@Modifying` 벌크 UPDATE는 단일 쿼리로 처리되어 이 문제를 피한다.

### 4.6 프론트엔드 (HTML)

정적 HTML 파일이 Spring Boot의 `/static` 폴더에 위치하며, 백엔드 API를 `fetch()`로 호출해 데이터를 동적으로 렌더링한다.

| 파일 | 화면 | 주요 기능 |
|------|------|----------|
| `index.html` | 로그인 | 회원번호/비밀번호 입력, 관리자/일반 회원 화면 분기 |
| `items.html` | 물품 목록 | AND/OR/NOT 복합 검색, 정렬, 카드형 목록 |
| `register.html` | 물품 등록 | FormData로 이미지 포함 multipart 전송, 미리보기 |
| `item_detail.html` | 물품 상세 | 이미지 갤러리, 구매 요청 폼, 1:1 채팅 진입 |
| `mypage.html` | 마이페이지 | 판매 중 물품 구매 요청 승인, 예약 중 물품 거래 완료 처리 |
| `chat_list.html` | 채팅 목록 | 참여 채팅방 목록, 안 읽은 메시지 수 배지 |
| `chat.html` | 채팅방 | 메시지 3초 폴링, 내/상대 메시지 좌우 정렬, 읽음 표시 |
| `admin_dashboard.html` | 관리자 통계 | ROLLUP/RANK 결과를 테이블로 시각화 |

**mypage.html — 비동기 렌더링 주의사항**

```javascript
// 잘못된 방식 (과거 코드): async 루프 중 await 시점에 DOM이 초기화됨
for (const item of items) {
    container.innerHTML += await buildCardHtml(item); // ← 이전 카드의 input 값 소멸
}

// 올바른 방식 (현재 코드): 전체 HTML을 배열에 모은 뒤 한 번만 갱신
const rendered = [];
for (const item of items) {
    rendered.push(await buildCardHtml(item));
}
container.innerHTML = rendered.join('');
```

**chat.html — 두 가지 진입 경로**

```
item_detail.html → chat.html?sellerCno=c1&itemNo=2
    ↓ initChat() → createOrGetRoom() → POST /api/chat/room
    ↓ roomNo 발급됨
    → location.replace('/chat.html?roomNo=3&partnerCno=c1')  ← redirect

chat_list.html → chat.html?roomNo=3&partnerCno=c1
    ↓ initChat() → fetchMessages() 바로 시작
```

---

## 5. API 전체 목록

| HTTP 메서드 | 경로 | 인증 필요 | 설명 |
|------------|------|----------|------|
| POST | /api/auth/login | ✗ | 로그인 |
| POST | /api/auth/logout | ✗ | 로그아웃 |
| POST | /api/items/register | ✓ | 물품 등록 |
| GET | /api/items/detail | ✗ | 물품 상세 조회 |
| GET | /api/items/my | ✓ | 내 물품 목록 |
| POST | /api/items/confirm-purchase | ✓ | 구매 확정 |
| GET | /api/items/search | ✗ | 복합 조건 검색 |
| POST | /api/requests/submit | ✓ | 구매 요청 제출 |
| GET | /api/requests/list | ✓ | 구매 요청 목록 조회 |
| POST | /api/transaction/approve | ✓ | 구매 요청 승인 |
| POST | /api/transaction/complete | ✓ | 거래 완료 처리 |
| POST | /api/chat/room | ✓ | 채팅방 생성/입장 |
| GET | /api/chat/list | ✓ | 채팅방 목록 조회 |
| POST | /api/chat/message | ✓ | 메시지 전송 |
| GET | /api/chat/messages/{roomNo} | ✓ | 메시지 조회 + 읽음 처리 |
| GET | /api/chat/me | ✓ | 현재 로그인 ID 조회 |
| GET | /api/admin/stat/rollup | ✓(c0) | ROLLUP 통계 |
| GET | /api/admin/stat/rank | ✓(c0) | RANK 통계 |
