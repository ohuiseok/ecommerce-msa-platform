package com.ecommerce.monolith.user.controller;

import com.ecommerce.monolith.user.dto.UserRequest;
import com.ecommerce.monolith.user.dto.UserResponse;
import com.ecommerce.monolith.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원가입, 로그인, 사용자 관리 API")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse.UserInfo> register(@Valid @RequestBody UserRequest.Register request) {
        UserResponse.UserInfo userInfo = userService.register(request);
        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse.LoginResponse> login(@Valid @RequestBody UserRequest.Login request) {
        UserResponse.LoginResponse loginResponse = userService.login(request);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse.UserInfo> getUser(@PathVariable Long userId, Authentication authentication) {
        validateSelfOrAdmin(userId, authentication);
        UserResponse.UserInfo userInfo = userService.getUserById(userId);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse.UserInfo> getUserByEmail(
            @PathVariable String email,
            Authentication authentication) {
        UserResponse.UserInfo userInfo = userService.getUserByEmail(email)
                .orElse(null);
        if (userInfo == null) {
            return ResponseEntity.notFound().build();
        }

        validateSelfOrAdmin(userInfo.getUserId(), authentication);
        return ResponseEntity.ok(userInfo);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse.UserInfo> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest.Update request,
            Authentication authentication) {
        validateSelfOrAdmin(userId, authentication);
        UserResponse.UserInfo userInfo = userService.updateUser(userId, request);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User API is running");
    }

    private void validateSelfOrAdmin(Long resourceUserId, Authentication authentication) {
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        if (!resourceUserId.equals(authenticatedUserId) && !isAdmin(authentication)) {
            throw new AccessDeniedException("접근 권한이 없습니다");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
