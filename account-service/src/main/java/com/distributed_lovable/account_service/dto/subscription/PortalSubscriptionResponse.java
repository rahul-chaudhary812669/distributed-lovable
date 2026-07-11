package com.distributed_lovable.account_service.dto.subscription;




import com.distributed_lovable.common_lib.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;


public record PortalSubscriptionResponse (
        String planName ,
        SubscriptionStatus status ,
        Instant currentPeriodStart,
        Instant currentPeriodEnd ,
        Boolean cancelAtPeriodEnd ,
        BigDecimal amount

){



}