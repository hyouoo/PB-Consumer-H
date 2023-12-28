package com.example.purebasketconsumer.consumer;

import com.example.purebasketconsumer.domain.cart.CartRepository;
import com.example.purebasketconsumer.domain.member.entity.Member;
import com.example.purebasketconsumer.domain.product.ProductRepository;
import com.example.purebasketconsumer.domain.product.entity.Product;
import com.example.purebasketconsumer.domain.purchase.PurchaseRepository;
import com.example.purebasketconsumer.consumer.dto.KafkaPurchaseDto;
import com.example.purebasketconsumer.domain.purchase.dto.PurchaseRequestDto.PurchaseDetail;
import com.example.purebasketconsumer.domain.purchase.entity.Purchase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseConsumer {
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    @KafkaListener(topics = "${spring.kafka.consumer.topics.purchase}", groupId = "${spring.kafka.consumer.group-id.purchase}",
            containerFactory = "kafkaPurchaseListenerContainerFactory" ,concurrency = "3")
    @Transactional
    public void purchaseProducts(@Payload KafkaPurchaseDto data,
                                 @Header(KafkaHeaders.OFFSET) List<Long> offsets,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topics
                                 ) {
        System.out.println("offsets = " + offsets);
        System.out.println("partitions = " + partitions);
        System.out.println("topics = " + topics);

        List<PurchaseDetail> purchaseDetails = data.purchaseRequestDto();
        Member member = data.member();
        List<Long> requestedProductsIds = purchaseDetails.stream().map(PurchaseDetail::productId).toList();
        List<Product> productList = productRepository.findByIdIn(requestedProductsIds);
        List<Purchase> purchaseList = new ArrayList<>();

        int size = purchaseDetails.size();

        for (int i = 0; i < size; i++) {
            Product product = productList.get(i);
            int amount = purchaseDetails.get(i).amount();
            Purchase purchase = Purchase.of(product, amount, member);
            purchaseList.add(purchase);
        }

        purchaseRepository.saveAll(purchaseList);
        cartRepository.deleteByMemberAndProductIdIn(member, requestedProductsIds);
        log.info("회원 {}: 상품 구매 완료 - {}", member.getId(), purchaseDetails);
    }

}
