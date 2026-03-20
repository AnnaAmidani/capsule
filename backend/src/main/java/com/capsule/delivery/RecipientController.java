package com.capsule.delivery;

import com.capsule.delivery.dto.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capsules/{capsuleId}/recipients")
public class RecipientController {

    private final DeliveryService deliveryService;

    public RecipientController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    public ResponseEntity<List<RecipientResponse>> add(
            @PathVariable UUID capsuleId,
            @RequestBody AddRecipientsRequest req,
            Authentication auth) {
        var recipients = deliveryService.addRecipients(capsuleId, userId(auth), req.emails());
        var response = recipients.stream()
                .map(r -> new RecipientResponse(r.getId(), r.getEmail(), r.getNotifiedAt(), r.getAccessedAt()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecipientResponse>> list(
            @PathVariable UUID capsuleId, Authentication auth) {
        var recipients = deliveryService.listRecipients(capsuleId, userId(auth));
        var response = recipients.stream()
                .map(r -> new RecipientResponse(r.getId(), r.getEmail(), r.getNotifiedAt(), r.getAccessedAt()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recipientId}")
    public ResponseEntity<Void> remove(
            @PathVariable UUID capsuleId,
            @PathVariable UUID recipientId,
            Authentication auth) {
        deliveryService.removeRecipient(capsuleId, recipientId, userId(auth));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
}
