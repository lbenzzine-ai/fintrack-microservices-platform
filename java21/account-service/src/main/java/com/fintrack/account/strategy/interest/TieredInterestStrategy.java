package com.fintrack.account.strategy.interest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Tier rates pulled from the {@code fintrack.account.interest.tiered} list in Config Server:
 *
 * <pre>
 * fintrack:
 *   account:
 *     interest:
 *       tiered:
 *         - { min: 0,      max: 1000,    rate: 0.005 }
 *         - { min: 1000,   max: 10000,   rate: 0.015 }
 *         - { min: 10000,  max: 100000,  rate: 0.025 }
 *         - { min: 100000, max: 9.9E18,  rate: 0.04  }
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "fintrack.account.interest")
public final class TieredInterestStrategy implements InterestStrategy {

    public static final String NAME = "tiered";

    private List<Tier> tiered = new ArrayList<>();

    @Override
    public String name() { return NAME; }

    @Override
    public BigDecimal compute(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal rate = tiered.stream()
                .filter(t -> principal.compareTo(t.min) >= 0 && principal.compareTo(t.max) < 0)
                .map(t -> t.rate)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal monthly = rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);
        return principal.multiply(monthly).multiply(BigDecimal.valueOf(months)).setScale(4, RoundingMode.HALF_EVEN);
    }

    public List<Tier> getTiered() { return tiered; }
    public void setTiered(List<Tier> tiered) { this.tiered = tiered; }

    public static class Tier {
        @JsonProperty BigDecimal min;
        @JsonProperty BigDecimal max;
        @JsonProperty BigDecimal rate;
        public BigDecimal getMin()  { return min; }
        public BigDecimal getMax()  { return max; }
        public BigDecimal getRate() { return rate; }
        public void setMin(BigDecimal v)  { this.min = v; }
        public void setMax(BigDecimal v)  { this.max = v; }
        public void setRate(BigDecimal v) { this.rate = v; }
    }
}
