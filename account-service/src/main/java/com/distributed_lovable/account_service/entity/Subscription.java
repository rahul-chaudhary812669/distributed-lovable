package com.distributed_lovable.account_service.entity;

import com.distributed_lovable.common_lib.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Subscription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(nullable = false , name="user_id")
    User user ;                                   // a user can have multiple subscriptions but only have one active subscription

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false , name="plan_id")
    Plan plan;

    @Enumerated(EnumType.STRING)
   SubscriptionStatus subscriptionStatus;


    @Column(unique = true)
    String razorpaySubscriptionId;           // razorpay subscription id

    Instant currentPeriodStart;
    Instant currentPeriodEnd;

    Boolean  cancelAtPeriodEnd = false;


    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;


}
