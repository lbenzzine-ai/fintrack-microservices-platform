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

    @Cacheable(value = "transactions:fees", key = "#ctx.type() + ':' + #ctx.amount() + ':' + #ctx.currencyCode() + ':' + #ctx.weekend()")
    public FeeQuote quote(FeeCalculationContext ctx) {
        FeeCalculationContext effective = ctx.withWeekend(isWeekend());
        FeeStrategy strategy = registry.strategyFor(effective.type());
        var fee = strategy.calculate(effective);
        return new FeeQuote(strategy.name(), effective.amount(), fee, effective.amount().add(fee));
    }

    @Transactional
    public FeeQuote computeAndAudit(String transactionUuid, FeeCalculationContext ctx) {
        FeeQuote quote = quote(ctx);
        auditRepository.save(FeeAudit.builder()
                .transactionUuid(transactionUuid)
                .strategy(quote.strategy())
                .principal(quote.principal())
                .fee(quote.fee())
                .build());
        log.debug("Fee audit tx={} strategy={} fee={}", transactionUuid, quote.strategy(), quote.fee());
        return quote;
    }

    private boolean isWeekend() {
        DayOfWeek d = LocalDate.now().getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }
}
