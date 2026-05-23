package com.company.fucomhgra.controller;

import com.company.fucomhgra.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        try {
            String email    = body.get("email");
            String name     = body.get("name");
            String googleId = body.get("googleId");

            // Validate input
            if (email == null || name == null || googleId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "email, name and googleId are required"
                ));
            }

            Map<String, String> response = authService.loginWithGoogle(
                    email, name, googleId
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", "Login failed: " + e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Validate Token
    // POST /api/auth/validate
    // ─────────────────────────────────────────────
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            var user = authService.getUserFromToken(token);

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "valid", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Get Current User
    // GET /api/auth/me
    // ─────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            var user = authService.getUserFromToken(token);

            return ResponseEntity.ok(Map.of(
                    "id",    user.getId(),
                    "email", user.getEmail(),
                    "name",  user.getName(),
                    "role",  user.getRole()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "message", "Unauthorized: " + e.getMessage()
            ));
        }
    }
}


