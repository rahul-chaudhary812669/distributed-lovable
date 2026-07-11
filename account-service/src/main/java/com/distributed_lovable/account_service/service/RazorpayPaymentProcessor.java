package com.distributed_lovable.account_service.service;

import com.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.distributed_lovable.account_service.dto.subscription.RazorpayCheckoutResponse;
import org.json.JSONObject;


public interface RazorpayPaymentProcessor {

    RazorpayCheckoutResponse createSubscription(CheckoutRequest request);
    void handleWebhookEvent(String type , JSONObject payload);


}
