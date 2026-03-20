package com.capsule.capsule;

import com.capsule.capsule.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleController {

    private final CapsuleService capsuleService;

    public CapsuleController(CapsuleService capsuleService) {
        this.capsuleService = capsuleService;
    }

    @PostMapping
    public ResponseEntity<CapsuleResponse> create(@Valid @RequestBody CreateCapsuleRequest req,
                                                   Authentication auth) {
        var capsule = capsuleService.create(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(CapsuleResponse.from(capsule));
    }

    @GetMapping
    public ResponseEntity<Page<CapsuleResponse>> list(
            @RequestParam(required = false) CapsuleState state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                capsuleService.listOwn(userId(auth), state, pageable).map(CapsuleResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CapsuleResponse> get(@PathVariable UUID id,
                                                @RequestParam(required = false) String token,
                                                Authentication auth) {
        Capsule capsule;
        if (token != null) {
            // TODO(Task 9): validate HMAC token via TokenService before granting access.
            // Token must be verified (signature, expiry, single-use via accessed_at) before
            // returning capsule contents. Until Task 9 is wired, this path is not secure.
            capsule = capsuleService.getAccessible(id);
        } else if (auth != null) {
            capsule = capsuleService.getForOwner(id, userId(auth));
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok(CapsuleResponse.from(capsule));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CapsuleResponse> update(@PathVariable UUID id,
                                                   @RequestBody UpdateCapsuleRequest req,
                                                   Authentication auth) {
        return ResponseEntity.ok(CapsuleResponse.from(capsuleService.update(id, userId(auth), req)));
    }

    @PostMapping("/{id}/seal")
    public ResponseEntity<CapsuleResponse> seal(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(CapsuleResponse.from(capsuleService.seal(id, userId(auth))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        capsuleService.delete(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items/upload-url")
    public ResponseEntity<UploadUrlResponse> uploadUrl(@PathVariable UUID id,
                                                        @RequestParam String contentType,
                                                        Authentication auth) {
        return ResponseEntity.ok(capsuleService.generateUploadUrl(id, userId(auth), contentType));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<CapsuleResponse.ItemResponse> addItem(@PathVariable UUID id,
                                                                  @Valid @RequestBody AddItemRequest req,
                                                                  Authentication auth) {
        var item = capsuleService.addItem(id, userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CapsuleResponse.ItemResponse(item.getId(), item.getType(),
                        item.getContent(), item.getS3Key(), item.getSortOrder()));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id, @PathVariable UUID itemId,
                                            Authentication auth) {
        capsuleService.deleteItem(id, itemId, userId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public")
    public ResponseEntity<Page<CapsuleResponse>> publicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(capsuleService.publicFeed(pageable).map(CapsuleResponse::from));
    }

    private UUID userId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }
}
