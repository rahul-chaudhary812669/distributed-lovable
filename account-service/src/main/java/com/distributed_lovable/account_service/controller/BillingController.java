package com.distributed_lovable.account_service.controller;

import com.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.distributed_lovable.account_service.dto.subscription.RazorpayCheckoutResponse;
import com.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.distributed_lovable.account_service.service.RazorpayPaymentProcessor;
import com.distributed_lovable.account_service.service.SubscriptionService;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BillingController {


    private final SubscriptionService subscriptionService;
    private final RazorpayPaymentProcessor razorpayPaymentProcessor;

    @Value("${razorpay.webhook.secret}")
    private String razorpayWebhookSecret ;


    @GetMapping("/api/me/subscription")
    public ResponseEntity<SubscriptionResponse>  getMySubscription(){
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription());
    }


    @PostMapping("/api/payments/checkout")
    public ResponseEntity<RazorpayCheckoutResponse>  createCheckoutResponse(
            @RequestBody CheckoutRequest request
    ){
        return ResponseEntity.ok(razorpayPaymentProcessor.createSubscription(request ));
    }


    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload ,
            @RequestHeader("X-Razorpay-Signature") String razorpaySignature
    ){
               log.info("handling webhook events");
        try {

            boolean isValid = Utils.verifyWebhookSignature(payload , razorpaySignature, razorpayWebhookSecret );

            if(!isValid){
                log.error("invalid razorpay webhook signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
            }


            JSONObject jsonPayload =
                    new JSONObject(payload);

            String event =
                    jsonPayload.getString("event");


            // contains actual subscription entity
            JSONObject payloadObject =
                    jsonPayload.getJSONObject("payload");

            razorpayPaymentProcessor.handleWebhookEvent(
                    event,
                    payloadObject
            );

            return ResponseEntity.ok("ok");

        } catch (Exception e) {

            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("webhook failed");

        }

    }







//    @PostMapping("/api/payments/portal")
//    public ResponseEntity<PortalResponse> openCustomerPortal(){
//        return ResponseEntity.ok(paymentProcessor.openCustomerPortal());
//
//    }


//    @PostMapping("/webhooks/payment")
//    public ResponseEntity<String> handlePaymentWebhooks(
//            @RequestBody String payload ,
//            @RequestHeader("Stripe-Signature") String sigHeader
//    ){
//        try {
//            Event event =  Webhook.constructEvent(payload , sigHeader , webhookSecret);
//
//            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
//            StripeObject  stripeObject = null ;
//
//            if(deserializer.getObject().isPresent()){
//                stripeObject = deserializer.getObject().get();
//            }else{
//                // fallback: deserialize from raw json
//                try{
//                    stripeObject = deserializer.deserializeUnsafe();
//                    if(stripeObject == null){
//                        log.warn(" failed to deserialize webhook object for event:{} ", event.getType());
//                        return ResponseEntity.ok().build();
//                    }
//
//                }catch(Exception e){
//                    log.error("unsafe deserialization failed for event {}:{}" , event.getType() , e.getMessage() );
//                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" deserialization failed");
//                }
//            }
//
//            // now extract metadata only if it is a checkout session
//            Map<String , String> metadata = new HashMap<>();
//            if(stripeObject instanceof  Session session){
//                metadata = session.getMetadata();
//            }
//
//            // pass to your processor
//            paymentProcessor.handleWebhookEvent(event.getType(), stripeObject , metadata);
//
//
//
//            return ResponseEntity.ok().build();
//
//        } catch (SignatureVerificationException e) {
//            throw new RuntimeException(e);
//        }
//
//
//    }







}



