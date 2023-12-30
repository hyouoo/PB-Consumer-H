package com.example.purebasketconsumer.domain.purchase.entity;

import com.example.purebasketconsumer.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "purchase")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purchase extends TimeStamp{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy="purchase", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseDetail> purchaseDetails = new ArrayList<>();

    @Column(nullable = false)
    private int totalPrice;

    @Builder
    private Purchase(Member member, int totalPrice) {

        this.member = member;
        this.totalPrice = totalPrice;
    }

    public static Purchase of(Member member, int totalPrice) {
        return Purchase.builder()
                .member(member)
                .totalPrice(totalPrice)
                .build();
    }

    public void addPurchaseDetails(List<PurchaseDetail> purchaseDetailList) {
        for (PurchaseDetail purchaseDetail : purchaseDetailList) {
            this.purchaseDetails.add(purchaseDetail);
            purchaseDetail.addPurchase(this);
        }
    }
}