package com.fintrack.account.repository;

import com.fintrack.account.entity.Currency;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Currency> findByCode(String code);
}
