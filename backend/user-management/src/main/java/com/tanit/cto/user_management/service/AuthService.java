package com.tanit.cto.user_management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tanit.cto.user_management.model.AuthRequest;
import com.tanit.cto.user_management.model.User;
import com.tanit.cto.user_management.repository.UserRepository;
import com.tanit.cto.user_management.security.JwtUtil;

import java.util.Optional;
import java.util.UUID;

// Service class to handle authentication logic
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PendingUserService pendingUserService;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PendingUserService pendingUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pendingUserService = pendingUserService;
    }

    // register new user pending email verification (not enabled/persistent in DB
    // yet)
    public User register(AuthRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setGender(request.getGender());
        user.addRole("USER");
        user.setEnabled(false);
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(System.currentTimeMillis() + 10 * 60 * 1000L); // 10min expiry
        pendingUserService.savePendingUser(verificationToken, user);

        return user;
    }

    // Generate JWT for an enabled user
    public String loginUser(User user) {
        if (!user.isEnabled()) {
            throw new RuntimeException("User email is not verified yet.");
        }
        String jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRoles(), 24 * 60 * 60 * 1000);
        return jwtToken;
    }

    // Verify user by token and save user in neo4j
    public String verifyUserAndLogin(String token) throws RuntimeException {
        User pendingUser = pendingUserService.getPendingUser(token);
        if (pendingUser == null) {
            Optional<User> existingUserOpt = userRepository.findByVerificationToken(token);
            if (existingUserOpt.isEmpty()) {
                throw new RuntimeException("Invalid verification token");
            }

            User existingUser = existingUserOpt.get();
            if (existingUser.isEnabled()) {
                return loginUser(existingUser);
            }

            throw new RuntimeException("User exists but not verified and not pending");
        }

        if (pendingUser.getVerificationTokenExpiry() < System.currentTimeMillis()) {
            pendingUserService.deletePendingUser(token);
            throw new RuntimeException("Verification token has expired");
        }
        pendingUser.setEnabled(true);
        pendingUser.setVerificationToken(null);
        pendingUser.setVerificationTokenExpiry(null);

        userRepository.save(pendingUser);

        String jwt = loginUser(pendingUser);

        pendingUserService.deletePendingUser(token);

        return jwt;
    }

    // Create password reset token
    public void createPasswordResetToken(User user) {
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(System.currentTimeMillis() + 60 * 60 * 1000L); // 1 hour expiry
        userRepository.save(user);
    }

    // Reset password using token
    public boolean resetPassword(String token, String newPassword) {
        return userRepository.findByResetToken(token)
                .map(user -> {
                    if (user.getResetTokenExpiry() != null && user.getResetTokenExpiry() > System.currentTimeMillis()) {
                        user.setPassword(passwordEncoder.encode(newPassword));
                        user.setResetToken(null);
                        user.setResetTokenExpiry(null);
                        userRepository.save(user);
                        return true;
                    }
                    return false;
                }).orElse(false);
    }

    // Validate user credentials
    public Optional<User> login(AuthRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> user.isEnabled())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()));
    }
}
