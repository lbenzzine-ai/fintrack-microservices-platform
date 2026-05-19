package com.fintrack.account.mapper;

import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.dto.BalanceResponse;
import com.fintrack.account.entity.Account;
import com.fintrack.account.entity.AccountStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private final AccountMapper mapper = new AccountMapperImpl();

    @Test
    void toResponse_copies_all_fields() {
        Instant created = Instant.parse("2024-01-01T00:00:00Z");
        Instant updated = Instant.parse("2024-02-01T00:00:00Z");
        Account a = Account.builder()
                .uuid("uuid-1")
                .userUuid("user-1")
                .balance(new BigDecimal("123.45"))
                .currencyCode("EUR")
                .status(AccountStatus.ACTIVE)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        AccountResponse r = mapper.toResponse(a);

        assertThat(r.uuid()).isEqualTo("uuid-1");
        assertThat(r.userUuid()).isEqualTo("user-1");
        assertThat(r.balance()).isEqualByComparingTo("123.45");
        assertThat(r.currencyCode()).isEqualTo("EUR");
        assertThat(r.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(r.createdAt()).isEqualTo(created);
        assertThat(r.updatedAt()).isEqualTo(updated);
    }

    @Test
    void toResponse_null_input_returns_null() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toBalance_maps_uuid_to_accountUuid() {
        Account a = Account.builder()
                .uuid("uuid-2")
                .userUuid("user-2")
                .balance(new BigDecimal("999.00"))
                .currencyCode("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        BalanceResponse r = mapper.toBalance(a);

        assertThat(r.accountUuid()).isEqualTo("uuid-2");
        assertThat(r.balance()).isEqualByComparingTo("999.00");
        assertThat(r.currencyCode()).isEqualTo("USD");
    }

    @Test
    void toBalance_null_input_returns_null() {
        assertThat(mapper.toBalance(null)).isNull();
    }
}
