package com.fintrack.notification.service;

import com.fintrack.notification.dto.NotificationResponse;
import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.entity.NotificationPreference;
import com.fintrack.notification.entity.NotificationStatus;
import com.fintrack.notification.exception.NotificationNotFoundException;
import com.fintrack.notification.mapper.NotificationMapper;
import com.fintrack.notification.repository.NotificationPreferenceRepository;
import com.fintrack.notification.repository.NotificationRepository;
import com.fintrack.notification.strategy.channel.DispatchResult;
import com.fintrack.notification.strategy.channel.NotificationStrategy;
import com.fintrack.notification.strategy.channel.NotificationStrategyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock NotificationMapper mapper;
    @Mock NotificationStrategyContext strategyContext;
    @Mock TemplateEngine templateEngine;
    @Mock NotificationStrategy strategy;

    @InjectMocks NotificationService service;

    private SendNotificationRequest req;

    @BeforeEach
    void setUp() {
        req = new SendNotificationRequest();
        req.setRecipient("to@example.com");
        req.setChannel(NotificationChannel.EMAIL);
        req.setTemplate("welcome");
        req.setSubject("Hi");
        req.setPayload(Map.of("name", "Alice"));
        req.setAccountUuid("acct-uuid");
        req.setTransactionUuid("tx-uuid");
    }

    @Test
    void create_success_persistsAndReturnsMappedResponse() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateEngine.process(eq("welcome"), any(IContext.class))).thenReturn("<html>Alice</html>");
        when(strategyContext.strategyFor(NotificationChannel.EMAIL)).thenReturn(strategy);
        when(strategy.send(any(Notification.class)))
                .thenReturn(DispatchResult.builder().delivered(true).provider("javamail-smtp").build());
        NotificationResponse expected = NotificationResponse.builder().uuid("u").build();
        when(mapper.toResponse(any(Notification.class))).thenReturn(expected);

        NotificationResponse out = service.create(req);

        assertThat(out).isSameAs(expected);

        // Two saves: initial persist + post-dispatch update.
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());

        Notification finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(finalSave.getDeliveryProvider()).isEqualTo("javamail-smtp");
        assertThat(finalSave.getSentAt()).isNotNull();
        assertThat(finalSave.getFailureReason()).isNull();
        assertThat(finalSave.getBody()).isEqualTo("<html>Alice</html>");
        assertThat(finalSave.getRecipient()).isEqualTo("to@example.com");
        assertThat(finalSave.getUuid()).isNotBlank();
    }

    @Test
    void create_dispatchFailure_marksFailed() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateEngine.process(eq("welcome"), any(IContext.class))).thenReturn("rendered");
        when(strategyContext.strategyFor(NotificationChannel.EMAIL)).thenReturn(strategy);
        when(strategy.send(any(Notification.class))).thenReturn(
                DispatchResult.builder().delivered(false).provider("javamail-smtp")
                        .failureReason("SMTP down").build());
        when(mapper.toResponse(any(Notification.class)))
                .thenReturn(NotificationResponse.builder().build());

        service.create(req);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        Notification finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(finalSave.getFailureReason()).isEqualTo("SMTP down");
        assertThat(finalSave.getSentAt()).isNull();
    }

    @Test
    void create_templateRenderThrows_fallsBackToInlineBody() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateEngine.process(eq("welcome"), any(IContext.class)))
                .thenThrow(new RuntimeException("template missing"));
        when(strategyContext.strategyFor(NotificationChannel.EMAIL)).thenReturn(strategy);
        when(strategy.send(any(Notification.class)))
                .thenReturn(DispatchResult.builder().delivered(true).provider("p").build());
        when(mapper.toResponse(any(Notification.class)))
                .thenReturn(NotificationResponse.builder().build());

        service.create(req);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        Notification firstSave = captor.getAllValues().get(0);
        assertThat(firstSave.getBody()).startsWith("[welcome]").contains("name=Alice");
    }

    @Test
    void create_renderedBody_isSavedOnNotification() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateEngine.process(eq("welcome"), any(IContext.class))).thenReturn("<html>RENDERED</html>");
        when(strategyContext.strategyFor(NotificationChannel.EMAIL)).thenReturn(strategy);
        when(strategy.send(any(Notification.class)))
                .thenReturn(DispatchResult.builder().delivered(true).provider("p").build());
        when(mapper.toResponse(any(Notification.class))).thenReturn(NotificationResponse.builder().build());

        service.create(req);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getBody()).isEqualTo("<html>RENDERED</html>");
    }

    @Test
    void create_nullPayload_doesNotNpeAndRendersWithEmptyContext() {
        SendNotificationRequest noPayload = new SendNotificationRequest();
        noPayload.setRecipient("to@x");
        noPayload.setChannel(NotificationChannel.EMAIL);
        noPayload.setTemplate("welcome");
        // payload, subject, accountUuid, transactionUuid intentionally left null
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateEngine.process(eq("welcome"), any(IContext.class))).thenReturn("body");
        when(strategyContext.strategyFor(NotificationChannel.EMAIL)).thenReturn(strategy);
        when(strategy.send(any(Notification.class)))
                .thenReturn(DispatchResult.builder().delivered(true).provider("p").build());
        when(mapper.toResponse(any(Notification.class))).thenReturn(NotificationResponse.builder().build());

        service.create(noPayload);

        verify(templateEngine).process(eq("welcome"), any(IContext.class));
    }

    @Test
    void upsertPreference_existing_appliesAllBooleanFlagsFromPatch() {
        NotificationPreference existing = NotificationPreference.builder()
                .userUuid("u1").emailEnabled(false).smsEnabled(false).pushEnabled(false).build();
        NotificationPreference patch = NotificationPreference.builder()
                .emailEnabled(true).smsEnabled(true).pushEnabled(true).build();
        when(preferenceRepository.findByUserUuid("u1")).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(existing)).thenReturn(existing);

        service.upsertPreference("u1", patch);

        assertThat(existing.isEmailEnabled()).isTrue();
        assertThat(existing.isSmsEnabled()).isTrue();
        assertThat(existing.isPushEnabled()).isTrue();
    }

    @Test
    void create_blankTemplate_skipsRendering() {
        req.setTemplate(""); // blank
        // @NotBlank is at the validation layer; the renderer guards with isBlank() too.
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(strategyContext.strategyFor(NotificationChannel.EMAIL)).thenReturn(strategy);
        when(strategy.send(any(Notification.class)))
                .thenReturn(DispatchResult.builder().delivered(true).provider("p").build());
        when(mapper.toResponse(any(Notification.class)))
                .thenReturn(NotificationResponse.builder().build());

        service.create(req);

        verify(templateEngine, never()).process(anyString(), any(IContext.class));
    }

    @Test
    void findByUuid_returnsMapped_whenFound() {
        Notification n = Notification.builder().uuid("u1").build();
        NotificationResponse resp = NotificationResponse.builder().uuid("u1").build();
        when(repository.findByUuid("u1")).thenReturn(Optional.of(n));
        when(mapper.toResponse(n)).thenReturn(resp);

        assertThat(service.findByUuid("u1")).isSameAs(resp);
    }

    @Test
    void findByUuid_throws_whenMissing() {
        when(repository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByUuid("missing"))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void findByAccount_mapsPage() {
        Notification n = Notification.builder().uuid("u1").build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(n), pageable, 1);
        when(repository.findByAccountUuid("acct", pageable)).thenReturn(page);
        when(mapper.toResponse(n)).thenReturn(NotificationResponse.builder().uuid("u1").build());

        Page<NotificationResponse> out = service.findByAccount("acct", pageable);

        assertThat(out.getContent()).hasSize(1);
        assertThat(out.getContent().get(0).getUuid()).isEqualTo("u1");
    }

    @Test
    void upsertPreference_existing_merges() {
        NotificationPreference existing = NotificationPreference.builder()
                .userUuid("u1").emailAddress("old@x").emailEnabled(false).smsEnabled(false).pushEnabled(false)
                .build();
        NotificationPreference patch = NotificationPreference.builder()
                .emailAddress("new@x").phoneNumber("+1").emailEnabled(true).smsEnabled(true).pushEnabled(false)
                .build();
        when(preferenceRepository.findByUserUuid("u1")).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(existing)).thenReturn(existing);

        NotificationPreference saved = service.upsertPreference("u1", patch);

        assertThat(saved.getEmailAddress()).isEqualTo("new@x");
        assertThat(saved.getPhoneNumber()).isEqualTo("+1");
        assertThat(saved.isEmailEnabled()).isTrue();
        assertThat(saved.isSmsEnabled()).isTrue();
        assertThat(saved.isPushEnabled()).isFalse();
    }

    @Test
    void upsertPreference_existing_preservesFields_whenPatchNull() {
        NotificationPreference existing = NotificationPreference.builder()
                .userUuid("u1").emailAddress("keep@x").phoneNumber("+keep").pushToken("tk")
                .build();
        NotificationPreference patch = NotificationPreference.builder().emailEnabled(true).build();
        when(preferenceRepository.findByUserUuid("u1")).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(existing)).thenReturn(existing);

        service.upsertPreference("u1", patch);

        assertThat(existing.getEmailAddress()).isEqualTo("keep@x");
        assertThat(existing.getPhoneNumber()).isEqualTo("+keep");
        assertThat(existing.getPushToken()).isEqualTo("tk");
        assertThat(existing.isEmailEnabled()).isTrue();
    }

    @Test
    void upsertPreference_new_setsUserUuidAndSaves() {
        NotificationPreference incoming = NotificationPreference.builder().emailEnabled(true).build();
        when(preferenceRepository.findByUserUuid("u1")).thenReturn(Optional.empty());
        when(preferenceRepository.save(incoming)).thenReturn(incoming);

        NotificationPreference saved = service.upsertPreference("u1", incoming);

        assertThat(saved.getUserUuid()).isEqualTo("u1");
        verify(preferenceRepository).save(incoming);
    }

    @Test
    void findPreference_delegates() {
        NotificationPreference pref = NotificationPreference.builder().userUuid("u").build();
        when(preferenceRepository.findByUserUuid("u")).thenReturn(Optional.of(pref));

        assertThat(service.findPreference("u")).contains(pref);
    }

    @ParameterizedTest
    @CsvSource({
            "EMAIL,true,false,false,true",
            "EMAIL,false,true,true,false",
            "SMS,true,true,false,true",
            "SMS,true,false,true,false",
            "PUSH,false,false,true,true",
            "PUSH,true,true,false,false",
    })
    void isOptedIn_respectsChannelFlag(String channel, boolean email, boolean sms, boolean push, boolean expected) {
        NotificationPreference pref = NotificationPreference.builder()
                .userUuid("u")
                .emailEnabled(email).smsEnabled(sms).pushEnabled(push)
                .build();
        when(preferenceRepository.findByUserUuid("u")).thenReturn(Optional.of(pref));

        assertThat(service.isOptedIn("u", NotificationChannel.valueOf(channel))).isEqualTo(expected);
    }

    @Test
    void isOptedIn_defaultsTrue_whenNoPreference() {
        when(preferenceRepository.findByUserUuid("u")).thenReturn(Optional.empty());

        assertThat(service.isOptedIn("u", NotificationChannel.EMAIL)).isTrue();
    }

    @Test
    void createAsync_delegatesToCreate() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("body");
        when(strategyContext.strategyFor(any())).thenReturn(strategy);
        when(strategy.send(any())).thenReturn(DispatchResult.builder().delivered(true).provider("p").build());
        when(mapper.toResponse(any())).thenReturn(NotificationResponse.builder().build());

        service.createAsync(req);

        verify(repository, times(2)).save(any(Notification.class));
    }

    @Test
    void create_strategyDispatch_isCalledForRequestedChannel() {
        req.setChannel(NotificationChannel.SMS);
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("body");
        when(strategyContext.strategyFor(NotificationChannel.SMS)).thenReturn(strategy);
        when(strategy.send(any())).thenReturn(DispatchResult.builder().delivered(true).provider("sms-gateway").build());
        when(mapper.toResponse(any())).thenReturn(NotificationResponse.builder().build());

        service.create(req);

        verify(strategyContext).strategyFor(NotificationChannel.SMS);
        verify(strategy).send(any(Notification.class));
    }
}
