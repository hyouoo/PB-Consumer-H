package com.example.purebasketconsumer.domain.cart;

import com.example.purebasketconsumer.domain.cart.entity.Cart;
import com.example.purebasketconsumer.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Cart c WHERE c.member = :member AND c.product.id IN :requestedProductsIds")
    void deleteByMemberAndProductIdIn(Member member, List<Long> requestedProductsIds);
}