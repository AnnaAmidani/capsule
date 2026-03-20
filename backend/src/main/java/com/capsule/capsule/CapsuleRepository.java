package com.capsule.capsule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;

public interface CapsuleRepository extends JpaRepository<Capsule, UUID> {
    Page<Capsule> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);
    Page<Capsule> findByOwnerIdAndStateOrderByCreatedAtDesc(UUID ownerId, CapsuleState state, Pageable pageable);
    Page<Capsule> findByStateAndVisibilityOrderByCreatedAtDesc(CapsuleState state, CapsuleVisibility visibility, Pageable pageable);
    List<Capsule> findByStateAndOpenAtBefore(CapsuleState state, Instant now);
}
