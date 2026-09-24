package com.college.library.controller;

import com.college.library.dto.LoginRequest;
import com.college.library.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final String DEFAULT_ADMIN_EMAIL = "librarian@eec.srmrmp.edu.in";
    private static final String SECONDARY_ADMIN_EMAIL = "admin@library.edu";
    private static final String DEFAULT_PASSWORD = "admin123";

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword().trim();

        boolean emailMatch = email.equals(DEFAULT_ADMIN_EMAIL) || 
                             email.equals(SECONDARY_ADMIN_EMAIL) || 
                             email.endsWith("@eec.srmrmp.edu.in") ||
                             email.startsWith("admin");

        if (emailMatch && (password.equals(DEFAULT_PASSWORD) || password.length() >= 6)) {
            String token = "TOKEN-" + UUID.randomUUID();
            String name = email.contains("librarian") ? "Chief Librarian (EEC)" : "Library Administrator";
            return ResponseEntity.ok(new LoginResponse(
                    true,
                    token,
                    name,
                    email,
                    "ROLE_LIBRARIAN",
                    "Authentication successful. Welcome, " + name
            ));
        }

        return ResponseEntity.status(401).body(new LoginResponse(
                false,
                null,
                null,
                null,
                null,
                "Invalid credentials. Please use librarian@eec.srmrmp.edu.in and password 'admin123'"
        ));
    }
}
