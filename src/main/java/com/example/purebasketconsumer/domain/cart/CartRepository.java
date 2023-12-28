package com.example.purebasketconsumer.domain.cart;

import com.example.purebasketconsumer.domain.cart.entity.Cart;
import com.example.purebasketconsumer.domain.member.entity.Member;
import com.example.purebasketconsumer.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    void deleteByMemberAndProductIdIn(Member member, List<Long> requestedProductsIds);
}