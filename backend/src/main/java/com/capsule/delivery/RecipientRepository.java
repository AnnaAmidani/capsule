package com.capsule.delivery;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    List<Recipient> findByCapsuleId(UUID capsuleId);
    List<Recipient> findByCapsuleIdAndNotifiedAtIsNull(UUID capsuleId);

    @Modifying
    @Query("UPDATE Recipient r SET r.accessedAt = :now WHERE r.id = :id AND r.accessedAt IS NULL")
    int markAccessed(@Param("id") UUID id, @Param("now") Instant now);
}
