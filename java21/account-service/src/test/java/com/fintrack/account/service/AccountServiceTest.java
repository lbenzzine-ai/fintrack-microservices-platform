package com.fintrack.account.service;

import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.dto.BalanceResponse;
import com.fintrack.account.dto.InterestPreview;
import com.fintrack.account.entity.Account;
import com.fintrack.account.entity.AccountStatus;
import com.fintrack.account.event.AccountCreatedEvent;
import com.fintrack.account.event.AccountDebitedEvent;
import com.fintrack.account.exception.AccountFrozenException;
import com.fintrack.account.exception.AccountNotFoundException;
import com.fintrack.account.exception.InsufficientFundsException;
import com.fintrack.account.mapper.AccountMapper;
import com.fintrack.account.messaging.KafkaMessagingStrategy;
import com.fintrack.account.messaging.MessagingStrategy;
import com.fintrack.account.messaging.MessagingStrategyRegistry;
import com.fintrack.account.repository.AccountRepository;
import com.fintrack.account.strategy.interest.FlatInterestStrategy;
import com.fintrack.account.strategy.interest.InterestStrategy;
import com.fintrack.account.strategy.interest.InterestStrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock AccountMapper accountMapper;
    @Mock MessagingStrategyRegistry messaging;
    @Mock InterestStrategyRegistry interestRegistry;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    // MessagingStrategy is a sealed interface — can't be mocked. Use real Kafka impl with a mock template.
    private MessagingStrategy messagingStrategy;
    // InterestStrategy is also sealed — use a real impl with a known flat rate.
    private InterestStrategy interestStrategy;

    @InjectMocks AccountService service;

    @BeforeEach
    void wireValues() {
        ReflectionTestUtils.setField(service, "defaultCurrency", "USD");
        ReflectionTestUtils.setField(service, "accountCreatedTopic", "fintrack.account.created");
        ReflectionTestUtils.setField(service, "accountDebitedTopic", "fintrack.account.debited");
        messagingStrategy = new KafkaMessagingStrategy(kafkaTemplate);
        // 0.05 flat rate keeps the math predictable in previewInterest tests.
        interestStrategy = new FlatInterestStrategy(new BigDecimal("0.05"));
    }

    private static Account account(String uuid, String userUuid, BigDecimal balance, AccountStatus status) {
        return Account.builder()
                .uuid(uuid)
                .userUuid(userUuid)
                .balance(balance)
                .status(status)
                .currencyCode("USD")
                .build();
    }

    private static AccountResponse stubResponse(String uuid, String userUuid) {
        return new AccountResponse(uuid, userUuid, BigDecimal.ZERO, "USD", AccountStatus.ACTIVE, null, null);
    }

    // ---------- createForUser ----------

    @Test
    void createForUser_existing_user_returns_existing_account_idempotently() {
        Account existing = account("acc-1", "user-1", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        AccountResponse mapped = stubResponse("acc-1", "user-1");

        when(accountRepository.existsByUserUuid("user-1")).thenReturn(true);
        when(accountRepository.findByUserUuid("user-1")).thenReturn(Optional.of(existing));
        when(accountMapper.toResponse(existing)).thenReturn(mapped);

        AccountResponse out = service.createForUser("user-1", "EUR");

        assertThat(out).isSameAs(mapped);
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(messaging);
    }

    @Test
    void createForUser_new_user_creates_active_zero_balance_and_publishes_event() {
        when(accountRepository.existsByUserUuid("user-2")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setUuid("acc-2");
            return a;
        });
        when(messaging.active()).thenReturn(messagingStrategy);

        AccountResponse mapped = stubResponse("acc-2", "user-2");
        when(accountMapper.toResponse(any(Account.class))).thenReturn(mapped);

        AccountResponse out = service.createForUser("user-2", "EUR");

        ArgumentCaptor<Account> savedCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(savedCaptor.capture());
        Account saved = savedCaptor.getValue();
        assertThat(saved.getUserUuid()).isEqualTo("user-2");
        assertThat(saved.getCurrencyCode()).isEqualTo("EUR");
        assertThat(saved.getBalance()).isEqualByComparingTo("0");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("fintrack.account.created"), eq("acc-2"), eventCaptor.capture());
        AccountCreatedEvent ev = (AccountCreatedEvent) eventCaptor.getValue();
        assertThat(ev.accountUuid()).isEqualTo("acc-2");
        assertThat(ev.userUuid()).isEqualTo("user-2");
        assertThat(ev.currencyCode()).isEqualTo("EUR");
        assertThat(ev.openingBalance()).isEqualByComparingTo("0");
        assertThat(ev.eventId()).isNotBlank();
        assertThat(ev.occurredAt()).isNotNull();

        assertThat(out).isSameAs(mapped);
    }

    @Test
    void createForUser_null_currency_falls_back_to_default() {
        when(accountRepository.existsByUserUuid("user-3")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setUuid("acc-3");
            return a;
        });
        when(messaging.active()).thenReturn(messagingStrategy);
        when(accountMapper.toResponse(any(Account.class))).thenReturn(stubResponse("acc-3", "user-3"));

        service.createForUser("user-3", null);

        ArgumentCaptor<Account> savedCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCurrencyCode()).isEqualTo("USD");
    }

    // ---------- findByUuid ----------

    @Test
    void findByUuid_returns_mapped_response() {
        Account a = account("acc-1", "user-1", new BigDecimal("500"), AccountStatus.ACTIVE);
        AccountResponse mapped = stubResponse("acc-1", "user-1");
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(accountMapper.toResponse(a)).thenReturn(mapped);

        assertThat(service.findByUuid("acc-1")).isSameAs(mapped);
    }

    @Test
    void findByUuid_missing_throws() {
        when(accountRepository.findByUuid("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByUuid("missing"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ---------- findByUserUuid ----------

    @Test
    void findByUserUuid_returns_mapped_response() {
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        AccountResponse mapped = stubResponse("acc-1", "user-1");
        when(accountRepository.findByUserUuid("user-1")).thenReturn(Optional.of(a));
        when(accountMapper.toResponse(a)).thenReturn(mapped);

        assertThat(service.findByUserUuid("user-1")).isSameAs(mapped);
    }

    @Test
    void findByUserUuid_missing_throws() {
        when(accountRepository.findByUserUuid("u")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByUserUuid("u")).isInstanceOf(AccountNotFoundException.class);
    }

    // ---------- balance ----------

    @Test
    void balance_returns_mapped_balance() {
        Account a = account("acc-1", "user-1", new BigDecimal("250"), AccountStatus.ACTIVE);
        BalanceResponse mapped = new BalanceResponse("acc-1", new BigDecimal("250"), "USD");
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(accountMapper.toBalance(a)).thenReturn(mapped);

        assertThat(service.balance("acc-1")).isSameAs(mapped);
    }

    @Test
    void balance_missing_throws() {
        when(accountRepository.findByUuid("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.balance("x")).isInstanceOf(AccountNotFoundException.class);
    }

    // ---------- debit ----------

    @Test
    void debit_sufficient_balance_subtracts_and_publishes_debited() {
        Account a = account("acc-1", "user-1", new BigDecimal("200.00"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));
        when(accountRepository.save(a)).thenReturn(a);
        when(messaging.active()).thenReturn(messagingStrategy);

        AccountDebitedEvent event = service.debit("acc-1", "tx-1",
                new BigDecimal("50.00"), new BigDecimal("1.00"));

        assertThat(a.getBalance()).isEqualByComparingTo("149.00");
        assertThat(event.accountUuid()).isEqualTo("acc-1");
        assertThat(event.amount()).isEqualByComparingTo("50.00");
        assertThat(event.fee()).isEqualByComparingTo("1.00");
        assertThat(event.newBalance()).isEqualByComparingTo("149.00");
        assertThat(event.transactionUuid()).isEqualTo("tx-1");
        assertThat(event.currencyCode()).isEqualTo("USD");
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.occurredAt()).isNotNull();

        verify(kafkaTemplate).send(eq("fintrack.account.debited"), eq("acc-1"), eq(event));
    }

    @Test
    void debit_null_fee_treated_as_zero() {
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));
        when(accountRepository.save(a)).thenReturn(a);
        when(messaging.active()).thenReturn(messagingStrategy);

        AccountDebitedEvent event = service.debit("acc-1", "tx-1", new BigDecimal("40"), null);

        assertThat(a.getBalance()).isEqualByComparingTo("60");
        assertThat(event.newBalance()).isEqualByComparingTo("60");
    }

    @Test
    void debit_exactBalanceMatchesAmountPlusFee_succeeds() {
        // Boundary: balance == amount+fee. Insufficient check uses compareTo < 0.
        Account a = account("acc-1", "user-1", new BigDecimal("105"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));
        when(accountRepository.save(a)).thenReturn(a);
        when(messaging.active()).thenReturn(messagingStrategy);

        service.debit("acc-1", "tx-edge", new BigDecimal("100"), new BigDecimal("5"));

        assertThat(a.getBalance()).isEqualByComparingTo("0");
        verify(accountRepository).save(a);
    }

    @Test
    void debit_balanceJustBelowAmountPlusFee_throws() {
        Account a = account("acc-1", "user-1", new BigDecimal("104.99"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", "tx-edge",
                new BigDecimal("100"), new BigDecimal("5")))
                .isInstanceOf(com.fintrack.account.exception.InsufficientFundsException.class);
    }

    @Test
    void debit_insufficient_funds_throws_and_no_publish() {
        Account a = account("acc-1", "user-1", new BigDecimal("10"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", "tx-1",
                new BigDecimal("100"), BigDecimal.ZERO))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("acc-1");

        verify(accountRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void debit_fee_pushing_over_balance_throws() {
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", "tx-1",
                new BigDecimal("99"), new BigDecimal("5")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void debit_frozen_account_throws_and_no_publish() {
        Account a = account("acc-1", "user-1", new BigDecimal("1000"), AccountStatus.FROZEN);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", "tx-1",
                new BigDecimal("10"), BigDecimal.ZERO))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("acc-1");

        verify(accountRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void debit_missing_account_throws() {
        when(accountRepository.findByUuidForUpdate("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.debit("nope", "tx", BigDecimal.ONE, BigDecimal.ZERO))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ---------- compensateCredit ----------

    @Test
    void compensateCredit_adds_amount_plus_fee_and_saves() {
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        service.compensateCredit("acc-1", new BigDecimal("40"), new BigDecimal("2"), "rollback");

        assertThat(a.getBalance()).isEqualByComparingTo("142");
        verify(accountRepository, times(1)).save(a);
    }

    @Test
    void compensateCredit_null_fee_treated_as_zero() {
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        service.compensateCredit("acc-1", new BigDecimal("25"), null, "rb");

        assertThat(a.getBalance()).isEqualByComparingTo("125");
    }

    @Test
    void compensateCredit_missing_account_throws() {
        when(accountRepository.findByUuidForUpdate("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.compensateCredit("x", BigDecimal.ONE, BigDecimal.ZERO, "rb"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ---------- previewInterest ----------

    @Test
    void previewInterest_uses_default_strategy_when_name_blank() {
        Account a = account("acc-1", "user-1", new BigDecimal("1000"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(interestRegistry.active()).thenReturn(interestStrategy);

        InterestPreview preview = service.previewInterest("acc-1", new BigDecimal("0.05"), 12, "");

        assertThat(preview.strategy()).isEqualTo("flat");
        assertThat(preview.principal()).isEqualByComparingTo("1000");
        // Flat 0.05 over 12 months on 1000 = exactly 50.0000
        BigDecimal expectedInterest = new BigDecimal("50").setScale(4, RoundingMode.HALF_EVEN);
        assertThat(preview.interest()).isEqualByComparingTo(expectedInterest);
        assertThat(preview.projectedBalance()).isEqualByComparingTo("1050.0000");
        verify(interestRegistry, never()).by(anyString());
    }

    @Test
    void previewInterest_uses_default_strategy_when_name_null() {
        Account a = account("acc-1", "user-1", new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(interestRegistry.active()).thenReturn(interestStrategy);

        InterestPreview preview = service.previewInterest("acc-1", new BigDecimal("0.01"), 6, null);
        assertThat(preview.strategy()).isEqualTo("flat");
        // 500 * (0.05/12) * 6 = 12.5
        assertThat(preview.interest()).isEqualByComparingTo("12.5000");
        assertThat(preview.projectedBalance()).isEqualByComparingTo("512.5000");
        verify(interestRegistry, never()).by(anyString());
    }

    @Test
    void previewInterest_uses_named_strategy_when_provided() {
        Account a = account("acc-1", "user-1", new BigDecimal("2000"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(interestRegistry.by("flat")).thenReturn(interestStrategy);

        InterestPreview preview = service.previewInterest("acc-1", new BigDecimal("0.05"), 12, "flat");

        assertThat(preview.strategy()).isEqualTo("flat");
        // 2000 * (0.05/12) * 12 = 100
        assertThat(preview.interest()).isEqualByComparingTo("100.0000");
        assertThat(preview.projectedBalance()).isEqualByComparingTo("2100.0000");
        verify(interestRegistry, never()).active();
    }

    @Test
    void previewInterest_missing_account_throws() {
        when(accountRepository.findByUuid("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.previewInterest("x", new BigDecimal("0.01"), 6, "flat"))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
