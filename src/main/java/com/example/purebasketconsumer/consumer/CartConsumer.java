package com.example.purebasketconsumer.consumer;

import com.example.purebasketconsumer.consumer.dto.KafkaPurchaseDto;
import com.example.purebasketconsumer.domain.cart.CartRepository;
import com.example.purebasketconsumer.domain.member.entity.Member;
import com.example.purebasketconsumer.domain.purchase.dto.PurchaseRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class CartConsumer {
    private final CartRepository cartRepository;


    @KafkaListener(topics = "${spring.kafka.consumer.topics.purchase}", groupId = "cart-consumer",
            containerFactory = "kafkaPurchaseListenerContainerFactory" ,concurrency = "3")
    @Transactional
    public void purchaseProducts(@Payload KafkaPurchaseDto data,
                                 @Header(KafkaHeaders.OFFSET) List<Long> offsets,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topics
    ) {
        log.info("topics: {}, partitions: {}, offsets: {}", topics, partitions, offsets);

        List<PurchaseRequestDto.PurchaseDetail> requestDetails = data.purchaseRequestDto();
        Member member = data.member();
        List<Long> requestedProductsIds = requestDetails.stream().map(PurchaseRequestDto.PurchaseDetail::productId).toList();

        cartRepository.deleteByMemberAndProductIdIn(member, requestedProductsIds);
        log.info("회원 {}: 장바구니 업데이트 완료 - {}", member.getId(), requestDetails);
    }
}
