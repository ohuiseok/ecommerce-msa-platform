package com.ecommerce.msa.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockUpdateRequest {
    private Integer quantity;
    private String operation; // INCREASE, DECREASE
}
