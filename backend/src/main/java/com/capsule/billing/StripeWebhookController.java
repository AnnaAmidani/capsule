package com.capsule.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/billing")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private static final Set<String> HANDLED_EVENTS = Set.of(
            "customer.subscription.created",
            "customer.subscription.updated",
            "customer.subscription.deleted"
    );

    private final SubscriptionService subscriptionService;
    private final String webhookSecret;

    public StripeWebhookController(SubscriptionService subscriptionService,
                                    @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.subscriptionService = subscriptionService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (!HANDLED_EVENTS.contains(event.getType())) {
            return ResponseEntity.ok().build();
        }

        var dataObject = event.getDataObjectDeserializer().getObject();
        if (dataObject.isEmpty()) return ResponseEntity.ok().build();

        var sub = (com.stripe.model.Subscription) dataObject.get();
        var status = mapStatus(sub.getStatus());
        // TODO(IaC): resolve tier from Stripe price metadata in production
        var tier = "seed";
        subscriptionService.handleSubscriptionUpdated(
                sub.getId(), status, tier, sub.getCurrentPeriodEnd());

        return ResponseEntity.ok().build();
    }

    private String mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> "active";
            case "past_due" -> "past_due";
            default -> "cancelled";
        };
    }
}
