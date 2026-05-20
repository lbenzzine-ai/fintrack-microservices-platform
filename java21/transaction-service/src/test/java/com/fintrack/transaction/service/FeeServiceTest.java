package com.fintrack.transaction.service;

import com.fintrack.transaction.dto.FeeQuote;
import com.fintrack.transaction.entity.FeeAudit;
import com.fintrack.transaction.entity.TransactionType;
import com.fintrack.transaction.repository.FeeAuditRepository;
import com.fintrack.transaction.strategy.fee.DomesticFeeStrategy;
import com.fintrack.transaction.strategy.fee.FeeCalculationContext;
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

    /** FeeStrategy is sealed in java21; mock a permitted final subtype instead. */
    @Mock DomesticFeeStrategy domesticStrategy;
    @Mock FeeStrategyRegistry registry;
    @Mock FeeAuditRepository auditRepository;
    @InjectMocks FeeService feeService;

    private FeeCalculationContext context(TransactionType type, BigDecimal amount) {
        return new FeeCalculationContext(type, amount, "USD", null, null, false, false);
    }

    @Test
    void quote_buildsQuoteFromStrategy() {
        FeeCalculationContext ctx = context(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("100"));
        when(registry.strategyFor(TransactionType.DOMESTIC_TRANSFER)).thenReturn(domesticStrategy);
        when(domesticStrategy.calculate(any())).thenReturn(new BigDecimal("0.50"));
        when(domesticStrategy.name()).thenReturn("domestic");

        FeeQuote quote = feeService.quote(ctx);

        assertThat(quote.strategy()).isEqualTo("domestic");
        assertThat(quote.principal()).isEqualByComparingTo("100");
        assertThat(quote.fee()).isEqualByComparingTo("0.50");
        assertThat(quote.total()).isEqualByComparingTo("100.50");
    }

    @Test
    void computeAndAudit_persistsAuditRow() {
        FeeCalculationContext ctx = context(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("1000"));
        when(registry.strategyFor(TransactionType.DOMESTIC_TRANSFER)).thenReturn(domesticStrategy);
        when(domesticStrategy.calculate(any())).thenReturn(new BigDecimal("5.00"));
        when(domesticStrategy.name()).thenReturn("domestic");

        FeeQuote q = feeService.computeAndAudit("tx-abc", ctx);

        ArgumentCaptor<FeeAudit> captor = ArgumentCaptor.forClass(FeeAudit.class);
        verify(auditRepository).save(captor.capture());
        FeeAudit saved = captor.getValue();
        assertThat(saved.getTransactionUuid()).isEqualTo("tx-abc");
        assertThat(saved.getStrategy()).isEqualTo("domestic");
        assertThat(saved.getPrincipal()).isEqualByComparingTo("1000");
        assertThat(saved.getFee()).isEqualByComparingTo("5.00");
        assertThat(q.total()).isEqualByComparingTo("1005.00");
    }
}
