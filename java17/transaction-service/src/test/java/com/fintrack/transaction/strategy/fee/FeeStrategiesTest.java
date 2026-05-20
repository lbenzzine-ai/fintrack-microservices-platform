package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeStrategiesTest {

    private static FeeCalculationContext ctx(TransactionType type, BigDecimal amount, boolean weekend) {
        return FeeCalculationContext.builder()
                .type(type)
                .amount(amount)
                .currencyCode("USD")
                .weekend(weekend)
                .build();
    }

    // ── DomesticFeeStrategy: 0.5% of amount, floored at 0.50 ───────────────────────────────
    @ParameterizedTest
    @CsvSource({
            "1000, 5.0000",     // 0.5% of 1000 = 5
            "100,  0.5000",     // 0.5% of 100 = 0.50 (at floor)
            "10,   0.5000",     // 0.5% of 10 = 0.05 → floored to 0.50
            "200,  1.0000",     // 0.5% of 200 = 1.0
            "0.01, 0.5000"      // negligible → floor
    })
    void domestic_percentWithFloor(BigDecimal amount, BigDecimal expected) {
        DomesticFeeStrategy s = new DomesticFeeStrategy(new BigDecimal("0.005"), new BigDecimal("0.50"));
        BigDecimal fee = s.calculate(ctx(TransactionType.DOMESTIC_TRANSFER, amount, false));
        assertThat(fee).isEqualByComparingTo(expected);
    }

    @Test
    void domestic_metadata() {
        DomesticFeeStrategy s = new DomesticFeeStrategy(new BigDecimal("0.005"), new BigDecimal("0.50"));
        assertThat(s.name()).isEqualTo("domestic");
        assertThat(s.supports()).containsExactly(TransactionType.DOMESTIC_TRANSFER);
    }

    // ── InternationalFeeStrategy: 1.5% + 5.00 surcharge ────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
            "1000, 20.0000",    // 15 + 5
            "100,  6.5000",     // 1.5 + 5
            "0,    5.0000"      // 0 + 5
    })
    void international_percentPlusSurcharge(BigDecimal amount, BigDecimal expected) {
        InternationalFeeStrategy s = new InternationalFeeStrategy(new BigDecimal("0.015"), new BigDecimal("5.00"));
        BigDecimal fee = s.calculate(ctx(TransactionType.INTERNATIONAL_TRANSFER, amount, false));
        assertThat(fee).isEqualByComparingTo(expected);
    }

    @Test
    void international_metadata() {
        InternationalFeeStrategy s = new InternationalFeeStrategy(new BigDecimal("0.015"), new BigDecimal("5.00"));
        assertThat(s.name()).isEqualTo("international");
        assertThat(s.supports()).containsExactly(TransactionType.INTERNATIONAL_TRANSFER);
    }

    // ── ATMWithdrawalFeeStrategy: 2.00 base, doubled on weekends ───────────────────────────
    @Test
    void atm_weekdayFee_isBase() {
        ATMWithdrawalFeeStrategy s = new ATMWithdrawalFeeStrategy(new BigDecimal("2.00"), new BigDecimal("2.0"));
        assertThat(s.calculate(ctx(TransactionType.ATM_WITHDRAWAL, new BigDecimal("100"), false)))
                .isEqualByComparingTo("2.00");
    }

    @Test
    void atm_weekendFee_isDoubled() {
        ATMWithdrawalFeeStrategy s = new ATMWithdrawalFeeStrategy(new BigDecimal("2.00"), new BigDecimal("2.0"));
        assertThat(s.calculate(ctx(TransactionType.ATM_WITHDRAWAL, new BigDecimal("100"), true)))
                .isEqualByComparingTo("4.000");
    }

    @Test
    void atm_metadata() {
        ATMWithdrawalFeeStrategy s = new ATMWithdrawalFeeStrategy(new BigDecimal("2.00"), new BigDecimal("2.0"));
        assertThat(s.name()).isEqualTo("atm");
        assertThat(s.supports()).containsExactly(TransactionType.ATM_WITHDRAWAL);
    }

    // ── BillPaymentFeeStrategy: flat 1.00 regardless of amount ─────────────────────────────
    @ParameterizedTest
    @CsvSource({"10", "100", "1000000"})
    void billPayment_isFlat(BigDecimal amount) {
        BillPaymentFeeStrategy s = new BillPaymentFeeStrategy(new BigDecimal("1.00"));
        assertThat(s.calculate(ctx(TransactionType.BILL_PAYMENT, amount, false)))
                .isEqualByComparingTo("1.00");
    }

    @Test
    void billPayment_metadata() {
        BillPaymentFeeStrategy s = new BillPaymentFeeStrategy(new BigDecimal("1.00"));
        assertThat(s.name()).isEqualTo("bill-payment");
        assertThat(s.supports()).containsExactly(TransactionType.BILL_PAYMENT);
    }

    // ── ZeroFeeStrategy: always zero ───────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({"1", "100", "9999"})
    void zero_alwaysZero(BigDecimal amount) {
        ZeroFeeStrategy s = new ZeroFeeStrategy();
        assertThat(s.calculate(ctx(TransactionType.INTERNAL_TRANSFER, amount, false))).isEqualByComparingTo("0");
    }

    @Test
    void zero_metadata() {
        ZeroFeeStrategy s = new ZeroFeeStrategy();
        assertThat(s.name()).isEqualTo("zero");
        assertThat(s.supports()).containsExactly(TransactionType.INTERNAL_TRANSFER);
    }
}
