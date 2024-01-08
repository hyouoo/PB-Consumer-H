package com.example.purebasketconsumer.consumer;

import com.example.purebasketconsumer.consumer.dto.KafkaPurchaseDto;
import com.example.purebasketconsumer.domain.cart.CartRepository;
import com.example.purebasketconsumer.domain.member.entity.Member;
import com.example.purebasketconsumer.domain.product.ProductRepository;
import com.example.purebasketconsumer.domain.product.entity.Product;
import com.example.purebasketconsumer.domain.purchase.PurchaseRepository;
import com.example.purebasketconsumer.domain.purchase.dto.PurchaseRequestDto;
import com.example.purebasketconsumer.domain.purchase.entity.Purchase;
import com.example.purebasketconsumer.domain.purchase.entity.PurchaseDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseConsumer {
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final CartRepository cartRepository;

    @KafkaListener(topics = "${spring.kafka.consumer.topics.purchase}", groupId = "${spring.kafka.consumer.group-id.purchase}",
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
        List<Product> productList = productRepository.findByIdIn(requestedProductsIds);
        List<PurchaseDetail> purchaseDetailList = new ArrayList<>();

        int size = requestDetails.size();
        int totalPrice = 0;
        for (int i = 0; i < size; i++) {
            Product product = productList.get(i);
            int amount = requestDetails.get(i).amount();
            int price = calculatePrice(product);
            totalPrice += price * amount;
            PurchaseDetail purchaseDetail = PurchaseDetail.from(product, price, amount);
            purchaseDetailList.add(purchaseDetail);
        }

        Purchase purchase = Purchase.of(member, totalPrice);
        purchase.addPurchaseDetails(purchaseDetailList);

        purchaseRepository.save(purchase);
        log.info("회원 {}: 상품 구매 완료 - {}", member.getId(), requestDetails);
    }


    private int calculatePrice(Product product) {
        return product.getPrice() * (100 - product.getDiscountRate()) / 100;
    }


//    public void purchaseBatchInsert(List<Purchase> purchaseList) {
//        String sql = "INSERT INTO purchase "
//                + "(amount, price, member_id, product_id, purchased_at) VALUE (?, ?, ?, ?, ?)";
//
//        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
//                    @Override
//                    public void setValues(PreparedStatement ps, int i) throws SQLException {
//                        Purchase purchase = purchaseList.get(i);
//                        ps.setInt(1, purchase.getAmount());
//                        ps.setInt(2, purchase.getPrice());
//                        ps.setLong(3, purchase.getMember().getId());
//                        ps.setLong(4, purchase.getProduct().getId());
//                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
//                    }
//
//                    @Override
//                    public int getBatchSize() {
//                        return purchaseList.size();
//                    }
//                }
//        );
//    }

}
