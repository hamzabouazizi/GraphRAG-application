package com.tanit.cto.user_management.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.tanit.cto.user_management.model.User;

import java.util.Optional;

public interface UserRepository extends Neo4jRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetToken(String token);
}
