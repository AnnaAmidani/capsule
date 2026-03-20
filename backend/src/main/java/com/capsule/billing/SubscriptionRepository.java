package com.capsule.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUserId(UUID userId);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
