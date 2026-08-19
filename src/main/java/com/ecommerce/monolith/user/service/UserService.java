package com.ecommerce.monolith.user.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.user.dto.UserRequest;
import com.ecommerce.monolith.user.dto.UserResponse;
import com.ecommerce.monolith.user.entity.User;
import com.ecommerce.monolith.user.repository.UserRepository;
import com.ecommerce.monolith.user.util.JwtUtil;
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
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole());
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
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.UserInfo.from(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse.UserInfo> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse.UserInfo::from);
    }

    public UserResponse.UserInfo updateUser(Long userId, UserRequest.Update request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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
