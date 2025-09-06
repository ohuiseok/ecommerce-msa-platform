package com.ecommerce.msa.order.client;

import com.ecommerce.msa.order.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {
    
    @GetMapping("/users/{userId}")
    UserResponse getUserById(@PathVariable Long userId);
}
