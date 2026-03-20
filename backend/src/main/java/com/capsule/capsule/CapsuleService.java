package com.capsule.capsule;

import com.capsule.capsule.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional
public class CapsuleService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleItemRepository itemRepository;
    private final S3Presigner s3Presigner;
    private final String s3Bucket;

    public CapsuleService(CapsuleRepository capsuleRepository,
                          CapsuleItemRepository itemRepository,
                          S3Presigner s3Presigner,
                          @Value("${aws.s3.bucket}") String s3Bucket) {
        this.capsuleRepository = capsuleRepository;
        this.itemRepository = itemRepository;
        this.s3Presigner = s3Presigner;
        this.s3Bucket = s3Bucket;
    }

    public Capsule create(UUID ownerId, CreateCapsuleRequest req) {
        var capsule = new Capsule();
        capsule.setOwnerId(ownerId);
        capsule.setTitle(req.title());
        capsule.setVisibility(req.visibility());
        capsule.setOpenAt(req.openAt());
        return capsuleRepository.save(capsule);
    }

    public Page<Capsule> listOwn(UUID ownerId, CapsuleState stateFilter, Pageable pageable) {
        if (stateFilter != null) {
            return capsuleRepository.findByOwnerIdAndStateOrderByCreatedAtDesc(ownerId, stateFilter, pageable);
        }
        return capsuleRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public Capsule getForOwner(UUID id, UUID ownerId) {
        var capsule = findOrThrow(id);
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your capsule");
        }
        return capsule;
    }

    @Transactional(readOnly = true)
    public Capsule getAccessible(UUID id) {
        var capsule = findOrThrow(id);
        if (capsule.getState() != CapsuleState.accessible) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Capsule is not accessible");
        }
        return capsule;
    }

    public Capsule update(UUID id, UUID ownerId, UpdateCapsuleRequest req) {
        var capsule = getForOwner(id, ownerId);
        requireDraft(capsule);
        if (req.title() != null) capsule.setTitle(req.title());
        if (req.openAt() != null) capsule.setOpenAt(req.openAt());
        return capsuleRepository.save(capsule);
    }

    public Capsule seal(UUID id, UUID ownerId) {
        var capsule = getForOwner(id, ownerId);
        requireDraft(capsule);
        capsule.setState(CapsuleState.sealed);
        return capsuleRepository.save(capsule);
    }

    public void delete(UUID id, UUID ownerId) {
        var capsule = getForOwner(id, ownerId);
        requireDraft(capsule);
        capsuleRepository.delete(capsule);
    }

    public UploadUrlResponse generateUploadUrl(UUID capsuleId, UUID ownerId, String contentType) {
        getForOwner(capsuleId, ownerId);
        var s3Key = "capsules/" + capsuleId + "/" + UUID.randomUUID();
        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(r -> r.bucket(s3Bucket).key(s3Key).contentType(contentType))
                .build();
        var url = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new UploadUrlResponse(url, s3Key);
    }

    public CapsuleItem addItem(UUID capsuleId, UUID ownerId, AddItemRequest req) {
        var capsule = getForOwner(capsuleId, ownerId);
        requireDraft(capsule);
        var item = new CapsuleItem();
        item.setCapsule(capsule);
        item.setType(req.type());
        item.setContent(req.content());
        item.setS3Key(req.s3Key());
        item.setSortOrder(req.sortOrder());
        return itemRepository.save(item);
    }

    public void deleteItem(UUID capsuleId, UUID itemId, UUID ownerId) {
        var capsule = getForOwner(capsuleId, ownerId);
        requireDraft(capsule);
        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        itemRepository.delete(item);
    }

    public Page<Capsule> publicFeed(Pageable pageable) {
        return capsuleRepository.findByStateAndVisibilityOrderByCreatedAtDesc(
                CapsuleState.accessible, CapsuleVisibility.public_, pageable);
    }

    private Capsule findOrThrow(UUID id) {
        return capsuleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found"));
    }

    private void requireDraft(Capsule capsule) {
        if (capsule.getState() != CapsuleState.draft) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Capsule is not in draft state");
        }
    }
}
