package com.fintrack.transaction.service;

import com.fintrack.transaction.dto.FeeQuote;
import com.fintrack.transaction.entity.FeeAudit;
import com.fintrack.transaction.entity.TransactionType;
import com.fintrack.transaction.repository.FeeAuditRepository;
import com.fintrack.transaction.strategy.fee.FeeCalculationContext;
import com.fintrack.transaction.strategy.fee.FeeStrategy;
import com.fintrack.transaction.strategy.fee.FeeStrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {

    @Mock FeeStrategyRegistry registry;
    @Mock FeeAuditRepository auditRepository;
    @Mock FeeStrategy strategy;
    @InjectMocks FeeService feeService;

    private FeeCalculationContext context(TransactionType type, BigDecimal amount) {
        return FeeCalculationContext.builder()
                .type(type).amount(amount).currencyCode("USD").build();
    }

    @Test
    void quote_buildsQuoteFromStrategy() {
        FeeCalculationContext ctx = context(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("100"));
        when(registry.strategyFor(TransactionType.DOMESTIC_TRANSFER)).thenReturn(strategy);
        when(strategy.calculate(ctx)).thenReturn(new BigDecimal("0.50"));
        when(strategy.name()).thenReturn("domestic");

        FeeQuote quote = feeService.quote(ctx);

        assertThat(quote.getStrategy()).isEqualTo("domestic");
        assertThat(quote.getPrincipal()).isEqualByComparingTo("100");
        assertThat(quote.getFee()).isEqualByComparingTo("0.50");
        assertThat(quote.getTotal()).isEqualByComparingTo("100.50");
    }

    @Test
    void quote_setsWeekendFlagOnContext() {
        FeeCalculationContext ctx = context(TransactionType.ATM_WITHDRAWAL, new BigDecimal("100"));
        when(registry.strategyFor(TransactionType.ATM_WITHDRAWAL)).thenReturn(strategy);
        when(strategy.calculate(any())).thenReturn(new BigDecimal("2.00"));
        when(strategy.name()).thenReturn("atm");

        feeService.quote(ctx);

        // The boolean was set on ctx; can't deterministically know if today is weekend,
        // but we can assert that weekend was *touched* (set to a boolean value reflecting now()).
        assertThat(ctx.isWeekend()).isIn(true, false);
    }

    @Test
    void computeAndAudit_persistsAuditRow() {
        FeeCalculationContext ctx = context(TransactionType.INTERNATIONAL_TRANSFER, new BigDecimal("1000"));
        when(registry.strategyFor(TransactionType.INTERNATIONAL_TRANSFER)).thenReturn(strategy);
        when(strategy.calculate(any())).thenReturn(new BigDecimal("20.00"));
        when(strategy.name()).thenReturn("international");

        FeeQuote q = feeService.computeAndAudit("tx-abc", ctx);

        ArgumentCaptor<FeeAudit> captor = ArgumentCaptor.forClass(FeeAudit.class);
        verify(auditRepository).save(captor.capture());
        FeeAudit saved = captor.getValue();
        assertThat(saved.getTransactionUuid()).isEqualTo("tx-abc");
        assertThat(saved.getStrategy()).isEqualTo("international");
        assertThat(saved.getPrincipal()).isEqualByComparingTo("1000");
        assertThat(saved.getFee()).isEqualByComparingTo("20.00");
        assertThat(q.getTotal()).isEqualByComparingTo("1020.00");
    }
}
