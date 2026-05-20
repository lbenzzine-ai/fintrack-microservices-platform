package com.fintrack.transaction.risk;

import com.fintrack.transaction.entity.Transaction;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngine {

    private final RiskRuleRegistry registry;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, namedThreadFactory("risk-rule-"));

    public RiskScore assess(Transaction tx) {
        List<RiskRule> rules = registry.all();
        List<Callable<Optional<RiskFinding>>> tasks = new ArrayList<>(rules.size());
        for (RiskRule rule : rules) {
            tasks.add(() -> rule.evaluate(tx));
        }

        List<RiskFinding> findings = new ArrayList<>();
        try {
            List<Future<Optional<RiskFinding>>> results = executor.invokeAll(tasks);
            for (Future<Optional<RiskFinding>> f : results) {
                try {
                    f.get().ifPresent(findings::add);
                } catch (ExecutionException ee) {
                    log.warn("risk rule threw — skipping", ee.getCause());
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("risk evaluation interrupted for tx={}", tx.getUuid());
        }

        RiskScore score = RiskScore.from(tx.getUuid(), findings);
        log.debug("risk assessed tx={} level={} score={} findings={}",
                tx.getUuid(), score.getLevel(), score.getScore(), findings.size());
        return score;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
