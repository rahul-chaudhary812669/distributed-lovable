package com.distributed_lovable.account_service.service.impl;


import com.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.distributed_lovable.account_service.entity.Plan;
import com.distributed_lovable.account_service.entity.Subscription;
import com.distributed_lovable.account_service.entity.User;
import com.distributed_lovable.account_service.mapper.SubscriptionMapper;
import com.distributed_lovable.account_service.repository.PlanRepository;
import com.distributed_lovable.account_service.repository.SubscriptionRepository;
import com.distributed_lovable.account_service.repository.UserRepository;
import com.distributed_lovable.account_service.service.SubscriptionService;
import com.distributed_lovable.common_lib.dto.PlanDto;
import com.distributed_lovable.common_lib.enums.SubscriptionStatus;
import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.common_lib.security.AuthUtil;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final AuthUtil authUtil;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final RazorpayClient razorpayClient;

    private final  Integer FREE_TIER_PROJECTS_ALLOWED  = 100 ;

    @Override
    public SubscriptionResponse getCurrentSubscription() {
         Long userId = authUtil.getCurrentUserId();
         var currentSubscription  =  subscriptionRepository.findByUserIdAndSubscriptionStatusIn(userId , Set.of(
            SubscriptionStatus.ACTIVE , SubscriptionStatus.PAST_DUE ,
                SubscriptionStatus.TRIALING
        )).orElse(
            new Subscription()
         );

         return subscriptionMapper.toSubscriptionResponse(currentSubscription);
    }


    @Override
    public void activateSubscription(Long userId, Long planId, String gatewaySubscriptionId, String customerId){

           Subscription subscription = getOrCreateSubscription(gatewaySubscriptionId , userId , planId );

           if(subscription.getSubscriptionStatus() != SubscriptionStatus.ACTIVE){
               subscription.setSubscriptionStatus(SubscriptionStatus.INCOMPLETE);
           }

           subscriptionRepository.save(subscription);
    }


    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {
             Subscription subscription =  getSubscription(gatewaySubscriptionId);
             if(subscription.getSubscriptionStatus() == SubscriptionStatus.CANCELLED) return ;
             subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
             subscriptionRepository.save(subscription);
    }


    @Override
    public void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd
                                            , Long userId , Long planId) {

          Subscription subscription =    getOrCreateSubscription(gatewaySubscriptionId , userId , planId) ;

        Instant newStart =  periodStart !=null ? periodStart : subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if(subscription.getSubscriptionStatus() == SubscriptionStatus.PAST_DUE ||
                                                 subscription.getSubscriptionStatus() == SubscriptionStatus.INCOMPLETE){
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        }

        subscriptionRepository.save(subscription);
    }



    @Override
    public void markSubscriptionPastDue(String gatewaySubscriptionId) {
             Subscription subscription =  getSubscription(gatewaySubscriptionId);

             if(subscription.getSubscriptionStatus() == SubscriptionStatus.PAST_DUE){
                 log.debug(" subscription is already past due , gatewaySubscriptionId {}", gatewaySubscriptionId);
                 return ;
             }

             subscription.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
              subscriptionRepository.save(subscription);
              // notify user via email
    }



    @Override
    public void subscriptionCompleted(String gatewaySubscriptionId) {
           Subscription subscription =  getSubscription(gatewaySubscriptionId);

        subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
    }



    @Override
    public void immediateCancelSubscription(String gatewaySubscriptionId) {
        Subscription subscription =  getSubscription(gatewaySubscriptionId);

        JSONObject request = new JSONObject();
        request.put("cancel_at_cycle_end", false);

        try {
            razorpayClient.subscriptions.cancel(gatewaySubscriptionId, request);
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }
        subscription.setSubscriptionStatus(
                SubscriptionStatus.CANCELLED
        );
        subscriptionRepository.save(subscription);
    }

    @Override
    public PlanDto getCurrentSubscribedPlanByUser() {
       SubscriptionResponse subscriptionResponse =   getCurrentSubscription();
       return subscriptionResponse.plan();
    }


    @Override
    @Transactional
    public void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus status,
                                   Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {

        Subscription subscription =  getSubscription(gatewaySubscriptionId);
        Boolean hasSubscriptionUpdated = false ;

        if(status != null && subscription.getSubscriptionStatus() != status){
            subscription.setSubscriptionStatus(status);
            hasSubscriptionUpdated = true ;
        }

        if(periodStart !=null && !periodStart.equals(subscription.getCurrentPeriodStart())){
            subscription.setCurrentPeriodStart(periodStart);
            hasSubscriptionUpdated = true ;
        }

        if(periodEnd !=null && !periodEnd.equals(subscription.getCurrentPeriodEnd())){
            subscription.setCurrentPeriodEnd(periodEnd);
            hasSubscriptionUpdated = true ;
        }

        if(cancelAtPeriodEnd!=null && !cancelAtPeriodEnd.equals(subscription.getCancelAtPeriodEnd())){
            subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            hasSubscriptionUpdated = true ;
        }

        if(planId != null && ! planId.equals(subscription.getPlan().getId())){
            subscription.setPlan( getPlan(planId));
            hasSubscriptionUpdated = true ;
        }

        if(hasSubscriptionUpdated){
            log.debug("subscription has beed updated {}", gatewaySubscriptionId);
            subscriptionRepository.save(subscription);
        }

    }



// @Override
//    public boolean canCreateNewProject() {
//
//        Long userId =  authUtil.getCurrentUserId();
//
//        SubscriptionResponse currentSubscription = getCurrentSubscription();
//
//        int countOfOwnedProjects = projectMemberRepository.countProjectOwnedByUser(userId);
//
//        if(currentSubscription.plan()==null){
//            return countOfOwnedProjects < FREE_TIER_PROJECTS_ALLOWED ;
//        }
//
//       return countOfOwnedProjects < currentSubscription.plan().maxProjects();
//
//    }       =>  TODO : shift in workspace service




    ///  utility methods

    private User getUser(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("user", userId.toString()));
    }

    private Plan getPlan(Long planId){
        return planRepository.findById(planId)
                .orElseThrow(()-> new ResourceNotFoundException("user", planId.toString()));
    }

    private Subscription getSubscription(String gatewaySubscriptionId) {
        return subscriptionRepository.findByRazorpaySubscriptionId(gatewaySubscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("subscription", gatewaySubscriptionId.toString()));
    }


    private Subscription getOrCreateSubscription(String gatewaySubscriptionId , Long userId , Long planId ){
            return  subscriptionRepository.findByRazorpaySubscriptionId(gatewaySubscriptionId).orElseGet(()->{
                     User user = getUser(userId);
                     Plan plan = getPlan(planId);
                  return Subscription.builder()
                           .razorpaySubscriptionId(gatewaySubscriptionId)
                           .user(user)
                           .plan(plan)
                          .subscriptionStatus(SubscriptionStatus.INCOMPLETE)
                           .build();
               });
    }



}


