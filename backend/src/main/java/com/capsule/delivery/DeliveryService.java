package com.capsule.delivery;

import com.capsule.capsule.*;
import com.capsule.delivery.dto.*;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeliveryService {

    private final RecipientRepository recipientRepository;
    private final CapsuleRepository capsuleRepository;
    private final TokenService tokenService;

    public DeliveryService(RecipientRepository recipientRepository,
                           CapsuleRepository capsuleRepository,
                           TokenService tokenService) {
        this.recipientRepository = recipientRepository;
        this.capsuleRepository = capsuleRepository;
        this.tokenService = tokenService;
    }

    public List<Recipient> addRecipients(UUID capsuleId, UUID ownerId, List<String> emails) {
        var capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found"));
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (capsule.getState() == CapsuleState.accessible || capsule.getState() == CapsuleState.archived) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add recipients to an accessible or archived capsule");
        }
        return emails.stream().map(email -> {
            var r = new Recipient();
            r.setCapsuleId(capsuleId);
            r.setEmail(email);
            return recipientRepository.save(r);
        }).toList();
    }

    public List<Recipient> listRecipients(UUID capsuleId, UUID ownerId) {
        var capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found"));
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return recipientRepository.findByCapsuleId(capsuleId);
    }

    public void removeRecipient(UUID capsuleId, UUID recipientId, UUID ownerId) {
        var capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (capsule.getState() != CapsuleState.draft) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only remove recipients from draft capsules");
        }
        recipientRepository.deleteById(recipientId);
    }

    /** Called by DeliveryConsumer. Transitions state first, then sends emails. */
    public void deliverCapsule(UUID capsuleId) {
        var capsule = capsuleRepository.findById(capsuleId).orElse(null);
        if (capsule == null || capsule.getState() != CapsuleState.sealed) return;

        // Step 1: transition state — idempotency boundary
        capsule.setState(CapsuleState.accessible);
        capsuleRepository.save(capsule);

        // Step 2: send emails to un-notified recipients only
        var recipients = recipientRepository.findByCapsuleIdAndNotifiedAtIsNull(capsuleId);
        for (var recipient : recipients) {
            try {
                var expires = Instant.now().plus(7, ChronoUnit.DAYS);
                var token = tokenService.generate(capsuleId, recipient.getEmail(), expires);
                recipient.setAccessToken(token);
                recipient.setTokenExpiresAt(expires);
                sendEmail(recipient.getEmail(), capsuleId, token);
                recipient.setNotifiedAt(Instant.now());
            } catch (Exception e) {
                recipient.setDeliveryError(e.getMessage());
            }
            recipientRepository.save(recipient);
        }
    }

    public Recipient validateTokenAndMarkAccessed(UUID capsuleId, String token) {
        var recipients = recipientRepository.findByCapsuleId(capsuleId);
        var recipient = recipients.stream()
                .filter(r -> token.equals(r.getAccessToken()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // Re-verify HMAC to ensure the token was legitimately issued
        if (recipient.getTokenExpiresAt() == null
                || !tokenService.verify(token, capsuleId, recipient.getEmail(), recipient.getTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        if (Instant.now().isAfter(recipient.getTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token has expired");
        }

        int updated = recipientRepository.markAccessed(recipient.getId(), Instant.now());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token has already been used");
        }
        return recipient;
    }

    private void sendEmail(String email, UUID capsuleId, String token) {
        var link = "https://capsule.app/open/" + capsuleId + "?token=" + token;
        LoggerFactory.getLogger(DeliveryService.class)
                .info("Delivery link for {}: {}", email, link);
    }
}
