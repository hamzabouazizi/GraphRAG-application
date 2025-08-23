package com.tanit.cto.user_management;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Controller exposing the /signup /login and /profile endpoints
@RestController
public class UserController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public UserController(AuthService authService, JwtUtil jwtUtil,
            AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    // Register a new user
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        User user = authService.register(request);
        return ResponseEntity.ok(user);
    }

    // Authenticate a user and returns a JWT token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpSession session) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        if (authentication.isAuthenticated()) {
            session.setAttribute("user", request.getEmail());

            String token = jwtUtil.generateToken(request.getEmail());
            return ResponseEntity.ok(new AuthResponse(token));
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    // Return the logged-in user details
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
