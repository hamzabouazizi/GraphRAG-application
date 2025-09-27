package com.tanit.cto.user_management.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.tanit.cto.user_management.model.User;

import java.util.Optional;

// Managing User entities in Neo4j with a custom finder by email
public interface UserRepository extends Neo4jRepository<User, String> {
    Optional<User> findByEmail(String email);
}
