package com.fintrack.user.repository;

import com.fintrack.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("select u from User u left join fetch u.roles where u.uuid = :uuid")
    Optional<User> findByUuidWithRoles(String uuid);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
