package com.ecommerce.msa.user.controller;

import com.ecommerce.msa.user.dto.UserRequest;
import com.ecommerce.msa.user.dto.UserResponse;
import com.ecommerce.msa.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
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
    public ResponseEntity<UserResponse.UserInfo> getUser(@PathVariable Long userId) {
        UserResponse.UserInfo userInfo = userService.getUserById(userId);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse.UserInfo> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse.UserInfo> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest.Update request) {
        UserResponse.UserInfo userInfo = userService.updateUser(userId, request);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running");
    }
}
