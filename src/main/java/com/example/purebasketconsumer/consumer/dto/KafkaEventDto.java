package com.example.purebasketconsumer.consumer.dto;

public record KafkaEventDto(
        String name,
        int price,
        int stock,
        int discountRate
) {
}