package com.example.purebasketconsumer.consumer;

import com.example.purebasketconsumer.consumer.dto.KafkaPurchaseDto;
import com.example.purebasketconsumer.domain.member.entity.Member;
import com.example.purebasketconsumer.domain.product.ProductRepository;
import com.example.purebasketconsumer.domain.product.entity.Product;
import com.example.purebasketconsumer.domain.purchase.dto.PurchaseRequestDto.PurchaseDetail;
import com.example.purebasketconsumer.domain.purchase.entity.Purchase;
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
    private final JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "${spring.kafka.consumer.topics.purchase}", groupId = "${spring.kafka.consumer.group-id.purchase}",
            containerFactory = "kafkaPurchaseListenerContainerFactory" ,concurrency = "3")
    @Transactional
    public void purchaseProducts(@Payload KafkaPurchaseDto data,
                                 @Header(KafkaHeaders.OFFSET) List<Long> offsets,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topics
                                 ) {
        log.info("topics: {}, partitions: {}, offsets: {}", topics, partitions, offsets);

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

        purchaseBatchInsert(purchaseList);
        cartBatchDelete(member, requestedProductsIds);
        log.info("회원 {}: 상품 구매 완료 - {}", member.getId(), purchaseDetails);
    }

    public void purchaseBatchInsert(List<Purchase> purchaseList) {
        String sql = "INSERT INTO purchase "
                + "(amount, price, member_id, product_id, purchased_at) VALUE (?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Purchase purchase = purchaseList.get(i);
                        ps.setInt(1, purchase.getAmount());
                        ps.setInt(2, purchase.getPrice());
                        ps.setLong(3, purchase.getMember().getId());
                        ps.setLong(4, purchase.getProduct().getId());
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    }

                    @Override
                    public int getBatchSize() {
                        return purchaseList.size();
                    }
                }
        );
    }

    public void cartBatchDelete(Member member, List<Long> requestedProductsIds) {
        String sql = "DELETE FROM cart WHERE member_id = ? AND product_id IN (?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, member.getId());
                ps.setLong(2, requestedProductsIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return requestedProductsIds.size();
            }
        });
    }

}
