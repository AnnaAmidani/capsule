package com.capsule.billing;

import com.capsule.user.*;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                UserRepository userRepository,
                                UserService userService,
                                @Value("${stripe.secret-key}") String stripeKey) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        Stripe.apiKey = stripeKey;
    }

    public String createCheckoutSession(UUID userId, String priceId, String successUrl, String cancelUrl)
            throws com.stripe.exception.StripeException {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(user.getEmail())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId).setQuantity(1L).build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .build();
        return Session.create(params).getUrl();
    }

    public String createPortalSession(UUID userId, String returnUrl)
            throws com.stripe.exception.StripeException {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getStripeCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Stripe customer found");
        }
        var params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(user.getStripeCustomerId())
                .setReturnUrl(returnUrl)
                .build();
        return com.stripe.model.billingportal.Session.create(params).getUrl();
    }

    public Subscription getSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription found"));
    }

    /** Called by webhook handler — idempotent via upsert on stripeSubscriptionId. */
    public void handleSubscriptionUpdated(String stripeSubId, String status,
                                           String tier, long periodEnd) {
        var existing = subscriptionRepository.findByStripeSubscriptionId(stripeSubId);
        var sub = existing.orElseGet(Subscription::new);
        sub.setStripeSubscriptionId(stripeSubId);
        sub.setStatus(SubscriptionStatus.valueOf(status));
        sub.setTier(UserTier.valueOf(tier));
        sub.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
        subscriptionRepository.save(sub);

        // Sync user tier and revoke refresh token on downgrade
        if (sub.getUserId() != null) {
            var user = userRepository.findById(sub.getUserId()).orElse(null);
            if (user != null && user.getTier() != sub.getTier()) {
                user.setTier(sub.getTier());
                userRepository.save(user);
                userService.invalidateRefreshTokenForUser(sub.getUserId());
            }
        }
    }
}
