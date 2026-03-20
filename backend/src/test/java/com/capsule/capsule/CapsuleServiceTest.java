package com.capsule.capsule;

import com.capsule.capsule.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapsuleServiceTest {

    @Mock CapsuleRepository capsuleRepository;
    @Mock CapsuleItemRepository itemRepository;
    @Mock S3Presigner s3Presigner;
    @InjectMocks CapsuleService capsuleService;

    @Test
    void createCapsuleSetsDraftState() {
        var ownerId = UUID.randomUUID();
        var req = new CreateCapsuleRequest("My Capsule", CapsuleVisibility.public_,
                Instant.now().plus(1, ChronoUnit.DAYS));
        when(capsuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var capsule = capsuleService.create(ownerId, req);

        assertThat(capsule.getState()).isEqualTo(CapsuleState.draft);
        assertThat(capsule.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void sealTransitionsDraftToSealed() {
        var capsule = new Capsule();
        capsule.setState(CapsuleState.draft);
        var ownerId = UUID.randomUUID();
        capsule.setOwnerId(ownerId);
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));
        when(capsuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var sealed = capsuleService.seal(UUID.randomUUID(), ownerId);

        assertThat(sealed.getState()).isEqualTo(CapsuleState.sealed);
    }

    @Test
    void sealRejectsNonOwner() {
        var capsule = new Capsule();
        capsule.setState(CapsuleState.draft);
        capsule.setOwnerId(UUID.randomUUID());
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));

        assertThatThrownBy(() -> capsuleService.seal(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
