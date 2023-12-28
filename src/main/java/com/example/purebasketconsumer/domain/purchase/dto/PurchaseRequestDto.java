package com.example.purebasketconsumer.domain.purchase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

public record PurchaseRequestDto(@NotNull List<PurchaseDetail> purchaseList) {

    @Builder
    public record PurchaseDetail(Long productId, int amount) {
    }
}