package com.ecommerce.msa.order.client;

import com.ecommerce.msa.order.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {
    
    @Override
    public UserResponse getUserById(Long userId) {
        log.warn("User service is unavailable. Using fallback for userId: {}", userId);
        return UserResponse.builder()
                .userId(userId)
                .name("사용자 정보 조회 실패")
                .email("fallback@example.com")
                .available(false)
                .build();
    }
}
