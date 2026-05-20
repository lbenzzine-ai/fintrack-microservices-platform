package com.fintrack.account.controller;

import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.dto.BalanceResponse;
import com.fintrack.account.dto.CreateAccountRequest;
import com.fintrack.account.dto.InterestPreview;
import com.fintrack.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock AccountService accountService;
    @InjectMocks AccountController controller;

    private static AccountResponse account() {
        return new AccountResponse("acc-1", "user-1", BigDecimal.ZERO, "USD", null, null, null);
    }

    @Test
    void shouldReturnCreatedAccountFromOpen() {
        AccountResponse expected = account();
        CreateAccountRequest req = new CreateAccountRequest("USD");
        when(accountService.createForUser("user-1", "USD")).thenReturn(expected);

        assertThat(controller.open("user-1", req)).isSameAs(expected);
    }

    @Test
    void shouldReturnAccountFromGet() {
        AccountResponse expected = account();
        when(accountService.findByUuid("acc-1")).thenReturn(expected);

        assertThat(controller.get("acc-1")).isSameAs(expected);
    }

    @Test
    void shouldReturnAccountFromMine() {
        AccountResponse expected = account();
        when(accountService.findByUserUuid("user-1")).thenReturn(expected);

        assertThat(controller.mine("user-1")).isSameAs(expected);
    }

    @Test
    void shouldReturnBalanceFromBalance() {
        BalanceResponse expected = new BalanceResponse("acc-1", new BigDecimal("100"), "USD");
        when(accountService.balance("acc-1")).thenReturn(expected);

        assertThat(controller.balance("acc-1")).isSameAs(expected);
    }

    @Test
    void shouldReturnInterestPreviewFromInterestPreview() {
        InterestPreview expected = InterestPreview.builder()
                .strategy("flat").principal(BigDecimal.ZERO)
                .interest(BigDecimal.ZERO).projectedBalance(BigDecimal.ZERO).build();
        when(accountService.previewInterest("acc-1", new BigDecimal("0.025"), 12, "flat"))
                .thenReturn(expected);

        assertThat(controller.interestPreview("acc-1", new BigDecimal("0.025"), 12, "flat"))
                .isSameAs(expected);
    }
}
