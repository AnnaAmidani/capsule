package com.capsule.capsule;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CapsuleItemRepository extends JpaRepository<CapsuleItem, UUID> {}
