package com.fintrack.account.repository;

import com.fintrack.account.entity.Account;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Account> findByUuid(String uuid);

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Account> findByUserUuid(String userUuid);

    /**
     * Pessimistic write-lock for the debit critical section to serialise concurrent debits
     * against the same row (defence in depth alongside JPA optimistic {@code @Version}).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.uuid = :uuid")
    Optional<Account> findByUuidForUpdate(@Param("uuid") String uuid);

    boolean existsByUserUuid(String userUuid);
}
