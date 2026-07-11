package com.distributed_lovable.account_service.service.impl;


import com.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.distributed_lovable.account_service.dto.subscription.RazorpayCheckoutResponse;
import com.distributed_lovable.account_service.entity.Plan;
import com.distributed_lovable.account_service.entity.User;
import com.distributed_lovable.account_service.repository.PlanRepository;
import com.distributed_lovable.account_service.repository.SubscriptionRepository;
import com.distributed_lovable.account_service.repository.UserRepository;
import com.distributed_lovable.account_service.service.RazorpayPaymentProcessor;
import com.distributed_lovable.account_service.service.SubscriptionService;
import com.distributed_lovable.common_lib.enums.SubscriptionStatus;
import com.distributed_lovable.common_lib.error.BadRequestException;
import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.common_lib.security.AuthUtil;
import com.razorpay.Customer;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults( level = AccessLevel.PRIVATE)
@Slf4j
public class RazorpayPaymentProcessorImpl implements RazorpayPaymentProcessor {

    final RazorpayClient razorpayClient;
    final PlanRepository planRepository;
    final UserRepository userRepository;
    final AuthUtil authUtil;
    final SubscriptionService subscriptionService;
    final SubscriptionRepository subscriptionRepository;

    @Value("${razorpay.key.id}")
     String keyId;

    @Override
    public RazorpayCheckoutResponse createSubscription(CheckoutRequest request) {

        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(()->  new ResourceNotFoundException("plan" , request.planId().toString()));

         Long userId = authUtil.getCurrentUserId();
         User user =   userRepository.findById(userId)
                                 .orElseThrow(()-> new ResourceNotFoundException("user" , userId.toString()));


       var  currentSubscription  =  subscriptionRepository.findByUserIdAndSubscriptionStatusIn(userId , Set.of(
                SubscriptionStatus.ACTIVE , SubscriptionStatus.PAST_DUE ,
                SubscriptionStatus.TRIALING
        ));

       if(currentSubscription.isPresent()){
           log.info(" an active subscription is already present for user {} with id {}" , userId , currentSubscription.get().getId());
            throw new BadRequestException(" you  already have an active  subscription , first cancel that then try ");
       }

        JSONObject subscriptionRequest = new JSONObject();

        JSONObject notes = new  JSONObject();
        notes.put("userId", userId);
        notes.put("planId", plan.getId());

        subscriptionRequest.put("plan_id" , plan.getRazorpayPlanId());
        subscriptionRequest.put("total_count" , 12);


        String razorpayCustomerId = user.getRazorpayCustomerId();
        if(razorpayCustomerId != null && !razorpayCustomerId.isBlank() ){
            subscriptionRequest.put("customer_id" , razorpayCustomerId);
        }else{
            JSONObject customerRequest =  new  JSONObject();
             customerRequest.put("name", user.getName());
             customerRequest.put("email", user.getUsername());

            try {
                Customer customer =    razorpayClient.customers.create(customerRequest);
                String customerId = customer.get("id");
                user.setRazorpayCustomerId(customerId);
                userRepository.save(user);
                subscriptionRequest.put("customer_id", customerId);

            } catch (RazorpayException e) {
                throw new RuntimeException(e);
            }

        }

        subscriptionRequest.put("notes", notes);

        try {
            Subscription subscription =  razorpayClient.subscriptions.create(subscriptionRequest);
            return new RazorpayCheckoutResponse(keyId,subscription.get("id"));
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }

    }



    @Override
    public void handleWebhookEvent(String type, JSONObject payload) {
               log.debug(" handling razorpay events {}", type);
               log.info("event {}", type);

               switch(type){
                    case "subscription.activated" ->  handlingSubscriptionActivated(payload);
                    case "subscription.updated" ->  handlingSubscriptionUpdated(payload);
                    case "subscription.cancelled" ->   handlingSubscriptionCancelled(payload);
                    case "subscription.charged" ->  handlingSubscriptionCharged(payload);
                    case "payment.failed" ->  handlingPaymentFailed(payload);
                    case "subscription.completed" -> handlingSubscriptionCompleted(payload);
                    case "subscription.halted" -> handlingSubscriptionHalted(payload);
                   default -> log.debug("Ignoring the event {}", type);
               }

    }




    private void  handlingSubscriptionActivated(JSONObject payload){
        log.info(" inside handlingSubscriptionActivated");
        if(payload ==null){
            log.error("payload object is null inside handlingSubscriptionAuthenticated ");
            return ;
        }

        JSONObject subscription =  payload.getJSONObject("subscription")
                                        .getJSONObject("entity");

        JSONObject notes =  subscription.getJSONObject("notes");

        Long userId = Long.valueOf(notes.get("userId").toString());
        Long planId = Long.valueOf(notes.get("planId").toString());

        String subscriptionId =  (String) subscription.get("id");
        String customerId =     (String) subscription.get("customer_id");

         subscriptionService.activateSubscription(userId,planId,subscriptionId,customerId);
    }


    private void  handlingSubscriptionCancelled(JSONObject payload){
        log.info("inside handlingSubscriptionCancelled");
        if(payload==null){
            log.error("payload object is null inside handleCustomerSubscriptionDeleted");
            return ;
        }

        JSONObject subscription =  payload.getJSONObject("subscription")
                .getJSONObject("entity");
        String subscriptionId =     subscription.getString("id");

        subscriptionService.cancelSubscription(subscriptionId);


    }

    private void handlingSubscriptionCharged(JSONObject payload){
        log.info("inside handlingSubscriptionCharged");
        if(payload==null){
            log.error("payload object is null inside handlingSubscriptionCharged");
            return ;
        }

        JSONObject subscription =  payload.getJSONObject("subscription")
                .getJSONObject("entity");

        JSONObject notes =  subscription.getJSONObject("notes");

        Long userId = Long.valueOf(notes.get("userId").toString());
        Long planId = Long.valueOf(notes.get("planId").toString());

        String subscriptionId =     subscription.getString("id");

        Instant periodStart = toInstant( subscription.getLong("current_start"));
        Instant periodEnd =  toInstant( subscription.getLong("current_end"));

        subscriptionService.renewSubscriptionPeriod(subscriptionId , periodStart , periodEnd , userId , planId);

    }

    private void handlingPaymentFailed(JSONObject payload){
        if(payload==null){
            log.error("payload object is null inside handlingPaymentFailed ");
            return ;
        }

        JSONObject subscription =  payload.getJSONObject("subscription")
                .getJSONObject("entity");

        String subscriptionId =     subscription.getString("id");

        subscriptionService.markSubscriptionPastDue(subscriptionId);


    }

    private void handlingSubscriptionCompleted( JSONObject payload){
        if(payload==null){
            log.error("payload object is null inside handlingSubscriptionCompleted ");
            return ;
        }
        JSONObject subscription =  payload.getJSONObject("subscription")
                .getJSONObject("entity");

        String subscriptionId =  subscription.getString("id");

        subscriptionService.subscriptionCompleted(subscriptionId);
    }


    private void handlingSubscriptionHalted( JSONObject payload){
        if(payload==null){
            log.error("payload object is null inside handlingSubscriptionHalted ");
            return ;
        }

        JSONObject subscription =  payload.getJSONObject("subscription")
                .getJSONObject("entity");

        String subscriptionId =  subscription.getString("id");

           subscriptionService.immediateCancelSubscription(subscriptionId);
    }



    private void handlingSubscriptionUpdated(JSONObject payload){
        log.info("inside handlingSubscriptionUpdated");
        if(payload ==null){
            log.error("payload object is null inside handlingSubscriptionUpdated ");
            return ;
        }

        JSONObject subscription =  payload.getJSONObject("subscription")
                .getJSONObject("entity");

        SubscriptionStatus status =  mapRazorpayStatusToEnum( (String) subscription.get("status"));
        if(status==null){
            log.warn("unknown status {} for subscription {}" ,  subscription.get("status"), subscription.get("id"));
            return ;
        }
        String subscriptionId =     subscription.getString("id");
        Boolean cancelAtCycleEnd =   subscription.getBoolean("cancel_at_cycle_end");

        Instant periodStart = toInstant((Long) subscription.get("current_start"));
        Instant periodEnd =  toInstant((Long) subscription.get("current_end"));

        String subscriptionPlanId =  (String) subscription.get("plan_id");
        Long planId =  planRepository.findByRazorpayPlanId(subscriptionPlanId).map(Plan::getId).orElse(null);


        subscriptionService.updateSubscription( subscriptionId,status,periodStart,periodEnd,cancelAtCycleEnd ,planId);
    }



    ///  utility methods



    private Instant toInstant(Long epoch){
        if(epoch!=null){
            return Instant.ofEpochSecond(epoch);
        }else{
            return null ;
        }
    }

    private SubscriptionStatus mapRazorpayStatusToEnum(String status) {
        if (status == null) {
            log.warn("Razorpay subscription status is null ");
            return null;
        }

        return switch (status) {
            case "active" ->  SubscriptionStatus.ACTIVE;
            case "authenticated" -> SubscriptionStatus.ACTIVE;
            case "created", "pending" -> SubscriptionStatus.INCOMPLETE;
            case "halted" ->  SubscriptionStatus.PAST_DUE;
            case "cancelled", "completed", "expired" ->  SubscriptionStatus.CANCELLED;
            default -> {
                log.warn("Unmapped Razorpay status {}", status);
                yield null;
            }
        };
    }


}
