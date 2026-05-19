package com.fintrack.account.service;

import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.dto.BalanceResponse;
import com.fintrack.account.dto.InterestPreview;
import com.fintrack.account.entity.Account;
import com.fintrack.account.entity.AccountStatus;
import com.fintrack.account.event.AccountCreatedEvent;
import com.fintrack.account.event.AccountCreditedEvent;
import com.fintrack.account.event.AccountDebitedEvent;
import com.fintrack.account.exception.AccountFrozenException;
import com.fintrack.account.exception.AccountNotFoundException;
import com.fintrack.account.exception.InsufficientFundsException;
import com.fintrack.account.mapper.AccountMapper;
import com.fintrack.account.messaging.MessagingStrategy;
import com.fintrack.account.messaging.MessagingStrategyRegistry;
import com.fintrack.account.repository.AccountRepository;
import com.fintrack.account.strategy.interest.InterestStrategy;
import com.fintrack.account.strategy.interest.InterestStrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    @Mock MessagingStrategy messagingStrategy;
    @Mock InterestStrategyRegistry interestRegistry;
    @Mock InterestStrategy interestStrategy;

    @InjectMocks AccountService service;

    @BeforeEach
    void wireValues() {
        ReflectionTestUtils.setField(service, "defaultCurrency", "USD");
        ReflectionTestUtils.setField(service, "accountCreatedTopic", "fintrack.account.created");
        ReflectionTestUtils.setField(service, "accountDebitedTopic", "fintrack.account.debited");
        ReflectionTestUtils.setField(service, "accountCreditedTopic", "fintrack.account.credited");
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

    // ---------- createForUser ----------

    @Test
    void createForUser_existing_user_returns_existing_account_idempotently() {
        Account existing = account("acc-1", "user-1", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        AccountResponse mapped = AccountResponse.builder().uuid("acc-1").userUuid("user-1").build();

        when(accountRepository.existsByUserUuid("user-1")).thenReturn(true);
        when(accountRepository.findByUserUuid("user-1")).thenReturn(Optional.of(existing));
        when(accountMapper.toResponse(existing)).thenReturn(mapped);

        AccountResponse out = service.createForUser("user-1", "EUR");

        assertThat(out).isSameAs(mapped);
        verify(accountRepository, never()).save(any());
        // No event must be published on the idempotent path.
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

        AccountResponse mapped = AccountResponse.builder().uuid("acc-2").userUuid("user-2").build();
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
        verify(messagingStrategy).publish(eq("fintrack.account.created"), eq("acc-2"), eventCaptor.capture());
        AccountCreatedEvent ev = (AccountCreatedEvent) eventCaptor.getValue();
        assertThat(ev.getAccountUuid()).isEqualTo("acc-2");
        assertThat(ev.getUserUuid()).isEqualTo("user-2");
        assertThat(ev.getCurrencyCode()).isEqualTo("EUR");
        assertThat(ev.getOpeningBalance()).isEqualByComparingTo("0");
        assertThat(ev.getEventId()).isNotBlank();
        assertThat(ev.getOccurredAt()).isNotNull();

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
        when(accountMapper.toResponse(any(Account.class))).thenReturn(AccountResponse.builder().build());

        service.createForUser("user-3", null);

        ArgumentCaptor<Account> savedCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCurrencyCode()).isEqualTo("USD");
    }

    // ---------- findByUuid ----------

    @Test
    void findByUuid_returns_mapped_response() {
        Account a = account("acc-1", "user-1", new BigDecimal("500"), AccountStatus.ACTIVE);
        AccountResponse mapped = AccountResponse.builder().uuid("acc-1").build();
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(accountMapper.toResponse(a)).thenReturn(mapped);

        assertThat(service.findByUuid("acc-1")).isSameAs(mapped);
    }

    @Test
    void findByUuid_missing_throws_AccountNotFoundException() {
        when(accountRepository.findByUuid("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByUuid("missing"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ---------- findByUserUuid ----------

    @Test
    void findByUserUuid_returns_mapped_response() {
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        AccountResponse mapped = AccountResponse.builder().uuid("acc-1").userUuid("user-1").build();
        when(accountRepository.findByUserUuid("user-1")).thenReturn(Optional.of(a));
        when(accountMapper.toResponse(a)).thenReturn(mapped);

        assertThat(service.findByUserUuid("user-1")).isSameAs(mapped);
    }

    @Test
    void findByUserUuid_missing_throws_AccountNotFoundException() {
        when(accountRepository.findByUserUuid("u")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByUserUuid("u"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ---------- balance ----------

    @Test
    void balance_returns_mapped_balance() {
        Account a = account("acc-1", "user-1", new BigDecimal("250"), AccountStatus.ACTIVE);
        BalanceResponse mapped = BalanceResponse.builder().accountUuid("acc-1").balance(new BigDecimal("250")).build();
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(accountMapper.toBalance(a)).thenReturn(mapped);

        assertThat(service.balance("acc-1")).isSameAs(mapped);
    }

    @Test
    void balance_missing_throws() {
        when(accountRepository.findByUuid("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.balance("x")).isInstanceOf(AccountNotFoundException.class);
    }

    // ---------- debit (single account, no transfer) ----------

    @Test
    void debit_sufficient_balance_subtracts_and_publishes_debited() {
        Account a = account("acc-1", "user-1", new BigDecimal("200.00"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));
        when(accountRepository.save(a)).thenReturn(a);
        when(messaging.active()).thenReturn(messagingStrategy);

        AccountDebitedEvent event = service.debit("acc-1", null, "tx-1",
                new BigDecimal("50.00"), new BigDecimal("1.00"));

        // 200 - (50 + 1) = 149
        assertThat(a.getBalance()).isEqualByComparingTo("149.00");
        assertThat(event.getAccountUuid()).isEqualTo("acc-1");
        assertThat(event.getAmount()).isEqualByComparingTo("50.00");
        assertThat(event.getFee()).isEqualByComparingTo("1.00");
        assertThat(event.getNewBalance()).isEqualByComparingTo("149.00");
        assertThat(event.getTransactionUuid()).isEqualTo("tx-1");
        assertThat(event.getCurrencyCode()).isEqualTo("USD");
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();

        verify(messagingStrategy).publish(eq("fintrack.account.debited"), eq("acc-1"), eq(event));
    }

    @Test
    void debit_null_fee_treated_as_zero() {
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));
        when(accountRepository.save(a)).thenReturn(a);
        when(messaging.active()).thenReturn(messagingStrategy);

        AccountDebitedEvent event = service.debit("acc-1", null, "tx-1",
                new BigDecimal("40"), null);

        assertThat(a.getBalance()).isEqualByComparingTo("60");
        assertThat(event.getNewBalance()).isEqualByComparingTo("60");
    }

    @Test
    void debit_exactBalanceMatchesAmountPlusFee_succeeds() {
        // Boundary: balance == amount+fee. Production uses compareTo(total) < 0 to reject.
        // Original returns false at equal → succeeds. Mutation flipping to <= 0 would reject.
        Account a = account("acc-1", "user-1", new BigDecimal("105"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));
        when(accountRepository.save(a)).thenReturn(a);
        when(messaging.active()).thenReturn(messagingStrategy);

        service.debit("acc-1", null, "tx-edge", new BigDecimal("100"), new BigDecimal("5"));

        assertThat(a.getBalance()).isEqualByComparingTo("0");
        verify(accountRepository).save(a);
    }

    @Test
    void debit_balanceJustBelowAmountPlusFee_throws() {
        // Boundary the other side: balance == amount+fee - 0.01 — should reject.
        Account a = account("acc-1", "user-1", new BigDecimal("104.99"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", null, "tx-edge",
                new BigDecimal("100"), new BigDecimal("5")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void debit_insufficient_funds_throws_and_no_publish() {
        Account a = account("acc-1", "user-1", new BigDecimal("10"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", null, "tx-1",
                new BigDecimal("100"), BigDecimal.ZERO))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("acc-1");

        verify(accountRepository, never()).save(any());
        verify(messagingStrategy, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void debit_fee_pushing_over_balance_throws() {
        // amount alone fits, but amount+fee does not — exercises the fee-inclusion branch.
        Account a = account("acc-1", "user-1", new BigDecimal("100"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", null, "tx-1",
                new BigDecimal("99"), new BigDecimal("5")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void debit_frozen_account_throws_and_no_publish() {
        Account a = account("acc-1", "user-1", new BigDecimal("1000"), AccountStatus.FROZEN);
        when(accountRepository.findByUuidForUpdate("acc-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.debit("acc-1", null, "tx-1",
                new BigDecimal("10"), BigDecimal.ZERO))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("acc-1");

        verify(accountRepository, never()).save(any());
        verify(messagingStrategy, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void debit_missing_account_throws() {
        when(accountRepository.findByUuidForUpdate("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.debit("nope", null, "tx", BigDecimal.ONE, BigDecimal.ZERO))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ---------- debit + credit (transfer) ----------

    @Test
    void debit_with_transfer_credits_destination_and_publishes_two_events() {
        Account src = account("acc-A", "userA", new BigDecimal("500"), AccountStatus.ACTIVE);
        Account dst = account("acc-B", "userB", new BigDecimal("100"), AccountStatus.ACTIVE);
        // src uuid "acc-A" < dst uuid "acc-B" → lock src first (else branch).
        when(accountRepository.findByUuidForUpdate("acc-A")).thenReturn(Optional.of(src));
        when(accountRepository.findByUuidForUpdate("acc-B")).thenReturn(Optional.of(dst));
        when(accountRepository.save(src)).thenReturn(src);
        when(accountRepository.save(dst)).thenReturn(dst);
        when(messaging.active()).thenReturn(messagingStrategy);

        AccountDebitedEvent event = service.debit("acc-A", "acc-B", "tx-9",
                new BigDecimal("75"), new BigDecimal("5"));

        assertThat(src.getBalance()).isEqualByComparingTo("420");   // 500 - 80
        assertThat(dst.getBalance()).isEqualByComparingTo("175");   // 100 + 75 (fee NOT credited)
        assertThat(event.getNewBalance()).isEqualByComparingTo("420");

        verify(messagingStrategy).publish(eq("fintrack.account.debited"), eq("acc-A"), any(AccountDebitedEvent.class));
        ArgumentCaptor<Object> creditedCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingStrategy).publish(eq("fintrack.account.credited"), eq("acc-B"), creditedCaptor.capture());
        AccountCreditedEvent credit = (AccountCreditedEvent) creditedCaptor.getValue();
        assertThat(credit.getAccountUuid()).isEqualTo("acc-B");
        assertThat(credit.getAmount()).isEqualByComparingTo("75");
        assertThat(credit.getNewBalance()).isEqualByComparingTo("175");
        assertThat(credit.getTransactionUuid()).isEqualTo("tx-9");
    }

    @Test
    void debit_with_transfer_locks_destination_first_when_uuid_lower() {
        // toAccountUuid "acc-A" < accountUuid "acc-B" → lock destination first (if branch).
        Account src = account("acc-B", "userB", new BigDecimal("500"), AccountStatus.ACTIVE);
        Account dst = account("acc-A", "userA", new BigDecimal("100"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-A")).thenReturn(Optional.of(dst));
        when(accountRepository.findByUuidForUpdate("acc-B")).thenReturn(Optional.of(src));
        when(accountRepository.save(src)).thenReturn(src);
        when(accountRepository.save(dst)).thenReturn(dst);
        when(messaging.active()).thenReturn(messagingStrategy);

        service.debit("acc-B", "acc-A", "tx", new BigDecimal("50"), BigDecimal.ZERO);

        assertThat(src.getBalance()).isEqualByComparingTo("450");
        assertThat(dst.getBalance()).isEqualByComparingTo("150");
    }

    @Test
    void debit_with_transfer_throws_when_destination_frozen() {
        Account src = account("acc-A", "userA", new BigDecimal("500"), AccountStatus.ACTIVE);
        Account dst = account("acc-B", "userB", new BigDecimal("100"), AccountStatus.FROZEN);
        when(accountRepository.findByUuidForUpdate("acc-A")).thenReturn(Optional.of(src));
        when(accountRepository.findByUuidForUpdate("acc-B")).thenReturn(Optional.of(dst));

        assertThatThrownBy(() -> service.debit("acc-A", "acc-B", "tx",
                new BigDecimal("10"), BigDecimal.ZERO))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("acc-B");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void debit_with_transfer_throws_when_destination_missing() {
        Account src = account("acc-A", "userA", new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuidForUpdate("acc-A")).thenReturn(Optional.of(src));
        when(accountRepository.findByUuidForUpdate("acc-B")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.debit("acc-A", "acc-B", "tx",
                new BigDecimal("10"), BigDecimal.ZERO))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("acc-B");
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
        when(interestStrategy.name()).thenReturn("tiered");
        when(interestStrategy.compute(eq(new BigDecimal("1000")), eq(new BigDecimal("0.05")), eq(12)))
                .thenReturn(new BigDecimal("50"));

        InterestPreview preview = service.previewInterest("acc-1", new BigDecimal("0.05"), 12, "");

        assertThat(preview.getStrategy()).isEqualTo("tiered");
        assertThat(preview.getPrincipal()).isEqualByComparingTo("1000");
        assertThat(preview.getInterest()).isEqualByComparingTo("50");
        assertThat(preview.getProjectedBalance()).isEqualByComparingTo("1050");
        verify(interestRegistry, never()).by(anyString());
    }

    @Test
    void previewInterest_uses_default_strategy_when_name_null() {
        Account a = account("acc-1", "user-1", new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(interestRegistry.active()).thenReturn(interestStrategy);
        when(interestStrategy.name()).thenReturn("flat");
        when(interestStrategy.compute(any(), any(), anyInt())).thenReturn(new BigDecimal("5"));

        InterestPreview preview = service.previewInterest("acc-1", new BigDecimal("0.01"), 6, null);
        assertThat(preview.getStrategy()).isEqualTo("flat");
        assertThat(preview.getProjectedBalance()).isEqualByComparingTo("505");
        verify(interestRegistry, never()).by(anyString());
    }

    @Test
    void previewInterest_uses_named_strategy_when_provided() {
        Account a = account("acc-1", "user-1", new BigDecimal("2000"), AccountStatus.ACTIVE);
        when(accountRepository.findByUuid("acc-1")).thenReturn(Optional.of(a));
        when(interestRegistry.by("compound")).thenReturn(interestStrategy);
        when(interestStrategy.name()).thenReturn("compound");
        when(interestStrategy.compute(eq(new BigDecimal("2000")), eq(new BigDecimal("0.05")), eq(12)))
                .thenReturn(new BigDecimal("102.30"));

        InterestPreview preview = service.previewInterest("acc-1", new BigDecimal("0.05"), 12, "compound");

        assertThat(preview.getStrategy()).isEqualTo("compound");
        assertThat(preview.getInterest()).isEqualByComparingTo("102.30");
        assertThat(preview.getProjectedBalance()).isEqualByComparingTo("2102.30");
        verify(interestRegistry, never()).active();
    }

    @Test
    void previewInterest_missing_account_throws() {
        when(accountRepository.findByUuid("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.previewInterest("x", new BigDecimal("0.01"), 6, "flat"))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
