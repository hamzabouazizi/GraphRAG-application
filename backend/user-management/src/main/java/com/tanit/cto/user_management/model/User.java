package com.tanit.cto.user_management.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("User") // Neo4j node entity
public class User {

    @Id
    private String email;

    private String password;

    private String fullName;

    private String gender;

    private Set<String> roles = new HashSet<>();

    private boolean enabled = false;
    private String verificationToken;
    private Long verificationTokenExpiry;
    private String resetToken;
    private Long resetTokenExpiry;

    public User() {
    }

    public User(String email, String password, String fullName, String gender) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void addRole(String role) {
        this.roles.add(role);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public Long getVerificationTokenExpiry() {
        return verificationTokenExpiry;
    }

    public void setVerificationTokenExpiry(Long verificationTokenExpiry) {
        this.verificationTokenExpiry = verificationTokenExpiry;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Long getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(Long resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }
}
