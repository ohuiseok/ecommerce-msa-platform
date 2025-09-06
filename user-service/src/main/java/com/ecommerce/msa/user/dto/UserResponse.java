package com.ecommerce.msa.user.dto;

import com.ecommerce.msa.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class UserResponse {

    @Data
    @Builder
    public static class UserInfo {
        private Long userId;
        private String email;
        private String name;
        private String phoneNumber;
        private User.UserStatus status;
        private User.UserRole role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static UserInfo from(User user) {
            return UserInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phoneNumber(user.getPhoneNumber())
                    .status(user.getStatus())
                    .role(user.getRole())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    public static class LoginResponse {
        private String accessToken;
        private String tokenType;
        private Long expiresIn;
        private UserInfo userInfo;
    }
}
