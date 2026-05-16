package com.fintrack.transaction.seed;

import com.fintrack.transaction.entity.Transaction;
import com.fintrack.transaction.entity.TransactionStatus;
import com.fintrack.transaction.entity.TransactionType;
import com.fintrack.transaction.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Optional bulk seeder — see java17 sibling for full doc. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fintrack.seed.enabled", havingValue = "true")
public class TransactionDataSeeder {

    private static final int BATCH = 1000;
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "JPY"};
    private static final TransactionStatus[] TERMINAL = {
            TransactionStatus.COMPLETED, TransactionStatus.COMPLETED, TransactionStatus.COMPLETED,
            TransactionStatus.COMPLETED, TransactionStatus.FAILED, TransactionStatus.COMPENSATED
    };

    private final TransactionRepository repo;

    @Value("${fintrack.seed.count:100000}")
    private int count;

    @Value("${fintrack.seed.from-accounts:1000}")
    private int sourceAccountPool;

    @PostConstruct
    @Transactional
    public void seed() {
        if (repo.count() > 0) {
            log.info("Seed skipped — transactions table already has {} rows", repo.count());
            return;
        }
        Faker faker = new Faker();
        log.info("Seeding {} transactions across {} synthetic source accounts…", count, sourceAccountPool);

        List<String> accounts = new ArrayList<>(sourceAccountPool);
        for (int i = 0; i < sourceAccountPool; i++) accounts.add(UUID.randomUUID().toString());

        List<Transaction> buffer = new ArrayList<>(BATCH);
        for (int i = 0; i < count; i++) {
            TransactionType type = TransactionType.values()[ThreadLocalRandom.current().nextInt(TransactionType.values().length)];
            BigDecimal amount = BigDecimal.valueOf(faker.number().numberBetween(5, 5000)).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal fee = amount.multiply(BigDecimal.valueOf(0.01)).setScale(4, RoundingMode.HALF_EVEN);
            String from = accounts.get(ThreadLocalRandom.current().nextInt(accounts.size()));
            String to = switch (type) {
                case ATM_WITHDRAWAL, BILL_PAYMENT -> null;
                default -> accounts.get(ThreadLocalRandom.current().nextInt(accounts.size()));
            };
            Instant when = Instant.now().minus(ThreadLocalRandom.current().nextInt(120), ChronoUnit.DAYS);

            Transaction t = Transaction.builder()
                    .uuid(UUID.randomUUID().toString())
                    .fromAccountUuid(from)
                    .toAccountUuid(to)
                    .amount(amount)
                    .fee(fee)
                    .currencyCode(CURRENCIES[ThreadLocalRandom.current().nextInt(CURRENCIES.length)])
                    .type(type)
                    .status(TERMINAL[ThreadLocalRandom.current().nextInt(TERMINAL.length)])
                    .description(faker.lorem().sentence(4))
                    .createdAt(when)
                    .updatedAt(when)
                    .build();
            buffer.add(t);

            if (buffer.size() == BATCH) {
                repo.saveAll(buffer);
                buffer.clear();
                if (i % 10000 == 0) log.info("…{} transactions persisted", i);
            }
        }
        if (!buffer.isEmpty()) repo.saveAll(buffer);
        log.info("Seed complete: {} transactions", repo.count());
    }
}
