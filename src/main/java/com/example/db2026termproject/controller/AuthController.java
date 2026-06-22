package com.example.db2026termproject.controller;

import com.example.db2026termproject.entity.Customer;
import com.example.db2026termproject.repository.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private CustomerRepository customerRepository;

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String cno, @RequestParam String passwd, HttpSession session) {
        // 1. DB에서 해당 아이디와 비밀번호가 일치하는 회원이 있는지 조회
        Customer customer = customerRepository.findByCnoAndPasswd(cno, passwd);

        if (customer != null) {
            // 2. 로그인 성공 시 세션에 회원번호 저장 (로그인 상태 유지)
            session.setAttribute("loggedInUser", customer.getCno());

            // 3. 화면(프론트엔드)으로 보낼 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인 성공");
            response.put("cno", customer.getCno());
            response.put("nickname", customer.getNickname());

            // ★ 4. 평가 기준: 관리자(c0)와 일반 회원 권한 및 화면 구분 로직
            if ("c0".equals(customer.getCno())) {
                response.put("role", "ADMIN");
                response.put("redirectUrl", "/admin/dashboard"); // 관리자는 통계 메뉴로 이동
            } else {
                response.put("role", "USER");
                response.put("redirectUrl", "/items"); // 일반 회원은 메인 물품 목록으로 이동
            }

            return ResponseEntity.ok(response);

        } else {
            // ★ 5. 예외 처리(평가 기준): 비정상 조작 시 튕기지 않고 에러 메시지 반환
            return ResponseEntity.status(401).body("회원번호 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    // 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // 기존 세션 정보 삭제
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}