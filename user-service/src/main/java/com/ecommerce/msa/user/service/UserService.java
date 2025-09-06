package com.ecommerce.msa.user.service;

import com.ecommerce.msa.user.dto.UserRequest;
import com.ecommerce.msa.user.dto.UserResponse;
import com.ecommerce.msa.user.entity.User;
import com.ecommerce.msa.user.repository.UserRepository;
import com.ecommerce.msa.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserResponse.UserInfo register(UserRequest.Register request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다");
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        return UserResponse.UserInfo.from(savedUser);
    }

    public UserResponse.LoginResponse login(UserRequest.Login request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("비활성화된 계정입니다");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getUserId());
        log.info("User logged in successfully: {}", user.getEmail());

        return UserResponse.LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .userInfo(UserResponse.UserInfo.from(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse.UserInfo getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return UserResponse.UserInfo.from(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse.UserInfo> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse.UserInfo::from);
    }

    public UserResponse.UserInfo updateUser(Long userId, UserRequest.Update request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getEmail());

        return UserResponse.UserInfo.from(updatedUser);
    }
}
