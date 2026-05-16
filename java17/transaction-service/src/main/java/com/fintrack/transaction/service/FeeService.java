package com.fintrack.transaction.service;

import com.fintrack.transaction.dto.FeeQuote;
import com.fintrack.transaction.entity.FeeAudit;
import com.fintrack.transaction.repository.FeeAuditRepository;
import com.fintrack.transaction.strategy.fee.FeeCalculationContext;
import com.fintrack.transaction.strategy.fee.FeeStrategy;
import com.fintrack.transaction.strategy.fee.FeeStrategyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeStrategyRegistry registry;
    private final FeeAuditRepository auditRepository;

    /** Quote the fee for a request without persisting anything — cheap, cacheable. */
    @Cacheable(value = "transactions:fees", key = "#ctx.type + ':' + #ctx.amount + ':' + #ctx.currencyCode + ':' + #ctx.weekend")
    public FeeQuote quote(FeeCalculationContext ctx) {
        ctx.setWeekend(isWeekend());
        FeeStrategy strategy = registry.strategyFor(ctx.getType());
        var fee = strategy.calculate(ctx);
        return FeeQuote.builder()
                .strategy(strategy.name())
                .principal(ctx.getAmount())
                .fee(fee)
                .total(ctx.getAmount().add(fee))
                .build();
    }

    /** Compute + persist an audit row keyed on the transaction. Called inside the tx-creation flow. */
    @Transactional
    public FeeQuote computeAndAudit(String transactionUuid, FeeCalculationContext ctx) {
        FeeQuote quote = quote(ctx);
        auditRepository.save(FeeAudit.builder()
                .transactionUuid(transactionUuid)
                .strategy(quote.getStrategy())
                .principal(quote.getPrincipal())
                .fee(quote.getFee())
                .build());
        log.debug("Fee audit tx={} strategy={} fee={}", transactionUuid, quote.getStrategy(), quote.getFee());
        return quote;
    }

    private boolean isWeekend() {
        DayOfWeek d = LocalDate.now().getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }
}
