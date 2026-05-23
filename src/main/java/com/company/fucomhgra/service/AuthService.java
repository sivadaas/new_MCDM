package com.company.fucomhgra.service;

import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    // Called after Google verifies the user
    public Map<String, String> loginWithGoogle(String email, String name, String googleId) {

        // Check if user already exists
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name, googleId));

        // Update googleId if missing
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return Map.of(
                "token", token,
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole()
        );
    }

    // Create a new user on first login
    private User createNewUser(String email, String name, String googleId) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setGoogleId(googleId);
        newUser.setRole("ENGINEER");
        return userRepository.save(newUser);
    }

    // Validate token and return user
    public User getUserFromToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
