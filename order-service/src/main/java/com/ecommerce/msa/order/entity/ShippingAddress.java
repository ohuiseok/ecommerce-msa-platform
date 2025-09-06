package com.ecommerce.msa.order.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    private String zipCode;
    private String address;
    private String detailAddress;
    private String recipientName;
    private String recipientPhone;
}
