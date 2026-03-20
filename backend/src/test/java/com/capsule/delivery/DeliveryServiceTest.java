package com.capsule.delivery;

import com.capsule.capsule.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock RecipientRepository recipientRepository;
    @Mock CapsuleRepository capsuleRepository;
    @Mock TokenService tokenService;
    @InjectMocks DeliveryService deliveryService;

    @Test
    void addRecipientsRejectsNonOwner() {
        var capsule = new Capsule();
        capsule.setOwnerId(UUID.randomUUID());
        capsule.setState(CapsuleState.draft);
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));

        assertThatThrownBy(() ->
            deliveryService.addRecipients(UUID.randomUUID(), UUID.randomUUID(), List.of("a@b.com")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deliverCapsuleSkipsAlreadyAccessible() {
        var capsule = new Capsule();
        capsule.setState(CapsuleState.accessible);
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));

        deliveryService.deliverCapsule(UUID.randomUUID());

        verify(capsuleRepository, never()).save(any());
    }

    @Test
    void deliverCapsuleTransitionsToAccessibleBeforeSendingEmails() {
        var capsuleId = UUID.randomUUID();
        var capsule = new Capsule();
        capsule.setState(CapsuleState.sealed);

        var recipient = new Recipient();
        recipient.setEmail("recipient@example.com");
        recipient.setCapsuleId(capsuleId);

        when(capsuleRepository.findById(capsuleId)).thenReturn(Optional.of(capsule));
        when(capsuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recipientRepository.findByCapsuleIdAndNotifiedAtIsNull(capsuleId))
            .thenReturn(List.of(recipient));
        when(tokenService.generate(any(), any(), any())).thenReturn("test-token");
        when(recipientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        deliveryService.deliverCapsule(capsuleId);

        assertThat(capsule.getState()).isEqualTo(CapsuleState.accessible);
        verify(capsuleRepository).save(capsule);
        assertThat(recipient.getNotifiedAt()).isNotNull();
    }

    @Test
    void validateTokenRejectsMissingToken() {
        when(recipientRepository.findByCapsuleId(any())).thenReturn(List.of());

        assertThatThrownBy(() ->
            deliveryService.validateTokenAndMarkAccessed(UUID.randomUUID(), "bad-token"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validateTokenRejectsExpiredToken() {
        var recipient = new Recipient();
        recipient.setAccessToken("my-token");
        recipient.setTokenExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(recipientRepository.findByCapsuleId(any())).thenReturn(List.of(recipient));

        assertThatThrownBy(() ->
            deliveryService.validateTokenAndMarkAccessed(UUID.randomUUID(), "my-token"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
