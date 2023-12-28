package com.example.purebasketconsumer.consumer.dto;

import com.example.purebasketconsumer.domain.member.entity.Member;
import com.example.purebasketconsumer.domain.purchase.dto.PurchaseRequestDto.PurchaseDetail;
import lombok.Builder;

import java.util.List;

@Builder
public record KafkaPurchaseDto(List<PurchaseDetail> purchaseRequestDto, Member member) {
}