package com.capsule.billing;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final SubscriptionService subscriptionService;

    public BillingController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> checkout(
            @RequestParam String priceId,
            @RequestParam String successUrl,
            @RequestParam String cancelUrl,
            Authentication auth) throws com.stripe.exception.StripeException {
        var url = subscriptionService.createCheckoutSession(userId(auth), priceId, successUrl, cancelUrl);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/subscription")
    public ResponseEntity<Subscription> subscription(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getSubscription(userId(auth)));
    }

    @PostMapping("/portal")
    public ResponseEntity<Map<String, String>> portal(
            @RequestParam String returnUrl, Authentication auth)
            throws com.stripe.exception.StripeException {
        var url = subscriptionService.createPortalSession(userId(auth), returnUrl);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
}
