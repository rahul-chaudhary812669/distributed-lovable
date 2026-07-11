package com.distributed_lovable.account_service.controller;


import com.distributed_lovable.account_service.dto.subscription.PortalSubscriptionResponse;
import com.distributed_lovable.account_service.service.CustomerPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customerPortal")
public class CustomerPortalController {

      private final CustomerPortalService customerPortalService;


    @GetMapping("/currentSubscription")
    public ResponseEntity<PortalSubscriptionResponse>  getCurrentSubscription(){
         return ResponseEntity.ok(customerPortalService.currentSubscription());
    }

    @PostMapping("/subscription/{subscriptionId}/cancelAtPeriodEnd")
     public ResponseEntity<Void> cancelAtPeriodEnd(@PathVariable Long subscriptionId){
         customerPortalService.cancelAtPeriodEnd(subscriptionId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/subscription/{subscriptionId}/resume")
    public ResponseEntity<Void> resume(@PathVariable Long subscriptionId){
        customerPortalService.resume(subscriptionId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/subscription/{subscriptionId}/cancel-immediately")
    public ResponseEntity<Void> immediateCancel(@PathVariable Long subscriptionId){
        customerPortalService.immediateCancel(subscriptionId);
        return ResponseEntity.ok().build();
    }





















}
