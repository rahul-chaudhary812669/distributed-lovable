package com.distributed_lovable.account_service.repository;

import com.distributed_lovable.account_service.entity.Subscription;
import com.distributed_lovable.common_lib.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;


public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {


    // get current active subscription
     Optional<Subscription>  findByUserIdAndSubscriptionStatusIn(Long userId, Set<SubscriptionStatus> status );

    boolean existsByRazorpaySubscriptionId(String gatewaySubscriptionId);

   Optional<Subscription> findByRazorpaySubscriptionId(String gatewaySubscriptionId);


}


