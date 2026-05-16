package com.fintrack.transaction.repository;

import com.fintrack.transaction.entity.FeeAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeAuditRepository extends JpaRepository<FeeAudit, Long> {
    List<FeeAudit> findByTransactionUuid(String transactionUuid);
}
