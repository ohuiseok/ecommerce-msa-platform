package com.ecommerce.msa.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String email;
    private String name;
    private String phoneNumber;
    private boolean available;
}
