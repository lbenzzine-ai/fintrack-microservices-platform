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
import com.fintrack.account.messaging.MessagingStrategyRegistry;
import com.fintrack.account.repository.AccountRepository;
import com.fintrack.account.strategy.interest.InterestStrategy;
import com.fintrack.account.strategy.interest.InterestStrategyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final MessagingStrategyRegistry messaging;
    private final InterestStrategyRegistry interestRegistry;

    @Value("${fintrack.account.default-currency:USD}")
    private String defaultCurrency;

    @Value("${fintrack.messaging.kafka.topics.account-created:fintrack.account.created}")
    private String accountCreatedTopic;

    @Value("${fintrack.messaging.kafka.topics.account-debited:fintrack.account.debited}")
    private String accountDebitedTopic;

    @Value("${fintrack.messaging.kafka.topics.account-credited:fintrack.account.credited}")
    private String accountCreditedTopic;

    /** Auto-create a wallet — called from the user-registered saga consumer (idempotent on userUuid). */
    @Transactional
    @CacheEvict(value = "accounts:byUser", key = "#userUuid")
    public AccountResponse createForUser(String userUuid, String currencyCode) {
        if (accountRepository.existsByUserUuid(userUuid)) {
            return accountRepository.findByUserUuid(userUuid).map(accountMapper::toResponse).orElseThrow();
        }
        Account account = Account.builder()
                .userUuid(userUuid)
                .currencyCode(currencyCode != null ? currencyCode : defaultCurrency)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
        Account saved = accountRepository.save(account);
        publishAccountCreated(saved);
        log.info("Created account uuid={} for user={}", saved.getUuid(), userUuid);
        return accountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "accounts:byId", key = "#uuid", unless = "#result == null")
    public AccountResponse findByUuid(String uuid) {
        Account a = accountRepository.findByUuid(uuid).orElseThrow(() -> new AccountNotFoundException(uuid));
        return accountMapper.toResponse(a);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "accounts:byUser", key = "#userUuid", unless = "#result == null")
    public AccountResponse findByUserUuid(String userUuid) {
        Account a = accountRepository.findByUserUuid(userUuid).orElseThrow(() -> new AccountNotFoundException(userUuid));
        return accountMapper.toResponse(a);
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(String uuid) {
        Account a = accountRepository.findByUuid(uuid).orElseThrow(() -> new AccountNotFoundException(uuid));
        return accountMapper.toBalance(a);
    }

    /**
     * Debit the source account and, when {@code toAccountUuid} is non-null, credit the destination
     * in the same DB transaction. Called from the transaction-initiated saga consumer.
     *
     * <p>Both rows are locked via {@code findByUuidForUpdate}; the lock order is sorted by UUID to
     * avoid trivial A→B / B→A deadlocks under concurrent transfers.
     *
     * <p>On success emits {@code account-debited} and, when applicable, {@code account-credited}.
     * Throws on insufficient funds / frozen / unknown so the saga consumer can publish
     * {@code transaction-failed}.
     */
    @Transactional
    @CacheEvict(value = {"accounts:byId","accounts:byUser","accounts:balance"}, allEntries = true)
    public AccountDebitedEvent debit(String accountUuid, String toAccountUuid, String transactionUuid,
                                     BigDecimal amount, BigDecimal fee) {
        // Lock both accounts in UUID order to avoid deadlock when two transfers cross.
        Account source;
        Account destination = null;
        if (toAccountUuid != null && toAccountUuid.compareTo(accountUuid) < 0) {
            destination = accountRepository.findByUuidForUpdate(toAccountUuid)
                    .orElseThrow(() -> new AccountNotFoundException(toAccountUuid));
            source = accountRepository.findByUuidForUpdate(accountUuid)
                    .orElseThrow(() -> new AccountNotFoundException(accountUuid));
        } else {
            source = accountRepository.findByUuidForUpdate(accountUuid)
                    .orElseThrow(() -> new AccountNotFoundException(accountUuid));
            if (toAccountUuid != null) {
                destination = accountRepository.findByUuidForUpdate(toAccountUuid)
                        .orElseThrow(() -> new AccountNotFoundException(toAccountUuid));
            }
        }

        if (source.getStatus() != AccountStatus.ACTIVE) throw new AccountFrozenException(accountUuid);
        if (destination != null && destination.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(toAccountUuid);
        }

        BigDecimal total = amount.add(fee == null ? BigDecimal.ZERO : fee);
        if (source.getBalance().compareTo(total) < 0) throw new InsufficientFundsException(accountUuid);

        source.setBalance(source.getBalance().subtract(total));
        Account savedSource = accountRepository.save(source);
        log.info("Debited account={} amount={} fee={} newBalance={} txId={}",
                accountUuid, amount, fee, savedSource.getBalance(), transactionUuid);

        Account savedDestination = null;
        if (destination != null) {
            destination.setBalance(destination.getBalance().add(amount));
            savedDestination = accountRepository.save(destination);
            log.info("Credited account={} amount={} newBalance={} txId={}",
                    toAccountUuid, amount, savedDestination.getBalance(), transactionUuid);
        }

        AccountDebitedEvent event = AccountDebitedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(transactionUuid)
                .accountUuid(savedSource.getUuid())
                .amount(amount)
                .fee(fee)
                .newBalance(savedSource.getBalance())
                .currencyCode(savedSource.getCurrencyCode())
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(accountDebitedTopic, savedSource.getUuid(), event);

        if (savedDestination != null) {
            AccountCreditedEvent credited = AccountCreditedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .transactionUuid(transactionUuid)
                    .accountUuid(savedDestination.getUuid())
                    .amount(amount)
                    .newBalance(savedDestination.getBalance())
                    .currencyCode(savedDestination.getCurrencyCode())
                    .occurredAt(Instant.now())
                    .build();
            messaging.active().publish(accountCreditedTopic, savedDestination.getUuid(), credited);
        }

        return event;
    }

    /** Compensation — re-credit on saga rollback. Idempotent at the event level. */
    @Transactional
    @CacheEvict(value = {"accounts:byId","accounts:byUser","accounts:balance"}, allEntries = true)
    public void compensateCredit(String accountUuid, BigDecimal amount, BigDecimal fee, String reason) {
        Account a = accountRepository.findByUuidForUpdate(accountUuid)
                .orElseThrow(() -> new AccountNotFoundException(accountUuid));
        BigDecimal total = amount.add(fee == null ? BigDecimal.ZERO : fee);
        a.setBalance(a.getBalance().add(total));
        accountRepository.save(a);
        log.warn("Compensation credit account={} amount={} reason={}", accountUuid, total, reason);
    }

    /**
     * Apply the configured {@link InterestStrategy} to preview interest for the period (no persistence).
     * Useful for the {@code /interest/preview} endpoint and for batch accruals.
     */
    @Transactional(readOnly = true)
    public InterestPreview previewInterest(String accountUuid, BigDecimal annualRate, int months, String strategyName) {
        Account a = accountRepository.findByUuid(accountUuid).orElseThrow(() -> new AccountNotFoundException(accountUuid));
        InterestStrategy strategy = (strategyName == null || strategyName.isBlank())
                ? interestRegistry.active()
                : interestRegistry.by(strategyName);
        BigDecimal interest = strategy.compute(a.getBalance(), annualRate, months);
        return InterestPreview.builder()
                .strategy(strategy.name())
                .principal(a.getBalance())
                .interest(interest)
                .projectedBalance(a.getBalance().add(interest))
                .build();
    }

    private void publishAccountCreated(Account account) {
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .accountUuid(account.getUuid())
                .userUuid(account.getUserUuid())
                .openingBalance(account.getBalance())
                .currencyCode(account.getCurrencyCode())
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(accountCreatedTopic, account.getUuid(), event);
    }
}
