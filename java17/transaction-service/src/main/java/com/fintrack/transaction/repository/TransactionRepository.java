package com.fintrack.transaction.repository;

import com.fintrack.transaction.entity.Transaction;
import com.fintrack.transaction.entity.TransactionStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Transaction> findByUuid(String uuid);

    Page<Transaction> findByFromAccountUuidOrToAccountUuid(String from, String to, Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    long countByStatus(TransactionStatus status);
}
