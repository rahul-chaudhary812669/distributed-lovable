package com.distributed_lovable.account_service.service.impl;

import com.distributed_lovable.account_service.dto.subscription.PortalSubscriptionResponse;
import com.distributed_lovable.account_service.entity.Subscription;
import com.distributed_lovable.account_service.repository.PlanRepository;
import com.distributed_lovable.account_service.repository.SubscriptionRepository;
import com.distributed_lovable.account_service.service.CustomerPortalService;
import com.distributed_lovable.common_lib.enums.SubscriptionStatus;
import com.distributed_lovable.common_lib.error.BadRequestException;
import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.common_lib.security.AuthUtil;
import com.razorpay.Plan;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerPortalServiceImpl implements CustomerPortalService {

    private final AuthUtil authUtil;
   private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RazorpayClient  razorpayClient;



    @Override
    public PortalSubscriptionResponse currentSubscription() {
        Long userId =  authUtil.getCurrentUserId();
        Subscription subscription =  subscriptionRepository.findByUserIdAndSubscriptionStatusIn(userId ,
                                                                            Set.of(SubscriptionStatus.ACTIVE,
                                                                            SubscriptionStatus.PAST_DUE ,
                                                                            SubscriptionStatus.INCOMPLETE ))
                .orElseThrow(()-> new BadRequestException("you don't have any active subscription"));

         String razorpayPlanId  = subscription.getPlan().getRazorpayPlanId();
         Plan plan = getRazorpayPlan(razorpayPlanId);

        JSONObject item = (JSONObject) plan.get("item");
        BigDecimal amount = BigDecimal.valueOf(item.getLong("amount")).divide(BigDecimal.valueOf(100));


         return new PortalSubscriptionResponse(subscription.getPlan().getName() , subscription.getSubscriptionStatus(),
                                               subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(),
                                                     subscription.getCancelAtPeriodEnd() , amount  );

    }


    @Override
    public void cancelAtPeriodEnd(Long subscriptionId) {

        Subscription subscription =  subscriptionRepository.findById(subscriptionId)
                .orElseThrow(()-> new ResourceNotFoundException("subscription " , subscriptionId.toString() ));

        if(subscription.getSubscriptionStatus() == SubscriptionStatus.CANCELLED){
            log.info(" you don't have an active subscription to cancel at period end ");
        }

        String razorpaySubscriptionId =   subscription.getRazorpaySubscriptionId();

             JSONObject request = new JSONObject();
             request.put("cancel_at_cycle_end" , true);

        try {

            razorpayClient.subscriptions.cancel(razorpaySubscriptionId , request );

        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }

        subscription.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(subscription);

    }


    @Override
    @Transactional
    public void resume(Long subscriptionId) {

        Subscription subscription =  subscriptionRepository.findById(subscriptionId)
                .orElseThrow(()-> new ResourceNotFoundException("subscription " , subscriptionId.toString() ));

        String razorpaySubscriptionId =  subscription.getRazorpaySubscriptionId();

        Instant currentTime = Instant.now();

        if(Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())
                && subscription.getCurrentPeriodEnd().isAfter(currentTime)) {

             JSONObject request = new JSONObject();
             request.put("cancel_at_cycle_end", false);

           try {

                razorpayClient.subscriptions.update(razorpaySubscriptionId , request);

                subscription.setCancelAtPeriodEnd(false);
                subscriptionRepository.save(subscription);

            } catch (RazorpayException e) {
                throw new RuntimeException(e);
            }


        }else{
               throw new BadRequestException("you don't  allow to resume subscription now ");
        }

    }



    @Override
    public void immediateCancel(Long subscriptionId) {
        Subscription subscription =  subscriptionRepository.findById(subscriptionId)
                .orElseThrow(()-> new ResourceNotFoundException("subscription " , subscriptionId.toString() ));

        if(subscription.getSubscriptionStatus() == SubscriptionStatus.CANCELLED){
            log.info(" you don't have an active subscription to cancel  ");
        }

        String razorpaySubscriptionId =   subscription.getRazorpaySubscriptionId();

        try {
            razorpayClient.subscriptions.cancel(razorpaySubscriptionId);
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }

    }



    /// internal methods
    ///
    ///




    Plan getRazorpayPlan(String planId){
        try {
           return  razorpayClient.plans.fetch(planId);
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }
    }



}
