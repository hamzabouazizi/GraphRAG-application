package com.tanit.cto.user_management.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import com.tanit.cto.user_management.model.AuthRequest;
import com.tanit.cto.user_management.model.User;
import com.tanit.cto.user_management.model.UserProfile;
import com.tanit.cto.user_management.repository.UserRepository;
import com.tanit.cto.user_management.security.JwtUtil;
import com.tanit.cto.user_management.service.AuthService;
import com.tanit.cto.user_management.service.EmailService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class UserController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Value("${jwt.cookie.secure}")
    private boolean jwtCookieSecure;

    @Autowired
    private EmailService emailService;

    public UserController(AuthService authService, JwtUtil jwtUtil,
            AuthenticationManager authenticationManager, UserRepository userRepository, EmailService emailService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Signup → send verification email
    @PostMapping("/api/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        User user = authService.register(request);
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
        return ResponseEntity.ok("Verification email sent to " + user.getEmail());
    }

    // Verify token → activate account → Login directly and return JWT in a cookie
    @PostMapping("/api/verifyAndLogin")
    public ResponseEntity<Map<String, String>> verifyAndLogin(@RequestParam String token,
            HttpServletResponse response) {
        try {
            ResponseCookie clearOldJwt = ResponseCookie.from("jwt", "")
                    .path("/")
                    .secure(true)
                    .httpOnly(true)
                    .sameSite("None")
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, clearOldJwt.toString());
            String jwt = authService.verifyUserAndLogin(token);
            ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .sameSite("None")
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("jwt", jwt));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Forgot password → send reset link
    @PostMapping("/api/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            authService.createPasswordResetToken(userOpt.get());
            emailService.sendResetPasswordEmail(userOpt.get().getEmail(), userOpt.get().getResetToken());
        }
        return ResponseEntity.ok("If the email exists, a reset link has been sent");
    }

    // Reset password → update password
    @PostMapping("/api/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("password");
        boolean success = authService.resetPassword(token, newPassword);
        if (success) {
            return ResponseEntity.ok("Password reset successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }
    }

    // Authenticate a user and returns a JWT token in a cookie
    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        if (!authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            return ResponseEntity.status(401).body("Email not verified");
        }

        ResponseCookie clearOldJwt = ResponseCookie.from("jwt", "")
                .path("/")
                .secure(true)
                .httpOnly(true)
                .sameSite("None")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearOldJwt.toString());

        String jwtToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRoles(),
                user.getFullName(),
                user.getGender(),
                24 * 60 * 60 * 1000);
        ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok("Login successful"); // no cookie in AuthResponse
    }

    // Return the logged-in user details
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> new UserProfile(
                        user.getEmail(),
                        user.getFullName(),
                        user.getGender(),
                        user.isEnabled()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok("Logged out successfully");
    }

}
