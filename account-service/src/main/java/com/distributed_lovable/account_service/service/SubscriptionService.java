package com.distributed_lovable.account_service.service;


import com.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.distributed_lovable.common_lib.dto.PlanDto;
import com.distributed_lovable.common_lib.enums.SubscriptionStatus;

import java.time.Instant;

public interface SubscriptionService {

     SubscriptionResponse getCurrentSubscription();

    void activateSubscription(Long userId, Long planId, String gatewaySubscriptionId, String customerId);

    void cancelSubscription(String gatewaySubscriptionId);

    void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd , Long userId , Long planId);

    void markSubscriptionPastDue(String gatewaySubscriptionId);

      void subscriptionCompleted(String gatewaySubscriptionId);

    void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus status,
                            Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId);

    void immediateCancelSubscription(String subscriptionId);


    PlanDto getCurrentSubscribedPlanByUser();
}












