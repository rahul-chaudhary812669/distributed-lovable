package com.distributed_lovable.account_service.service;


import com.distributed_lovable.account_service.dto.subscription.PortalSubscriptionResponse;

public interface CustomerPortalService {

     PortalSubscriptionResponse currentSubscription();

    void cancelAtPeriodEnd(Long subscriptionId);

    void resume(Long subscriptionId);

    void immediateCancel(Long subscriptionId);

}



