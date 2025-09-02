package com.paymentprovider.paymentservice.controller;

import com.paymentprovider.paymentservice.security.AuthenticationService;
import com.paymentprovider.paymentservice.security.AuthenticationResponse;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticates a user and returns JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthenticationResponse response = authenticationService.authenticateUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getMerchantId()
            );

            return ResponseEntity.ok(new LoginResponse(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getUser().getUserId(),
                    response.getUser().getUsername(),
                    response.getUser().getMerchantId(),
                    response.getUser().getRoles()
            ));
        } catch (BadCredentialsException e) {
            logger.warn("Login failed for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid username or password"));
        } catch (Exception e) {
            logger.error("Login error for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("LOGIN_ERROR", "An error occurred during login"));
        }
    }

    /**
     * Refreshes an access token using a refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String newAccessToken = authenticationService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(new RefreshTokenResponse(newAccessToken));
        } catch (JwtException e) {
            logger.debug("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_REFRESH_TOKEN", "Invalid or expired refresh token"));
        } catch (Exception e) {
            logger.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("REFRESH_ERROR", "An error occurred during token refresh"));
        }
    }

    /**
     * Generates an API key for the authenticated user.
     */
    @PostMapping("/api-key")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<?> generateApiKey() {
        try {
            String apiKey = authenticationService.generateApiKey();
            return ResponseEntity.ok(new ApiKeyResponse(apiKey));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            logger.error("API key generation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("API_KEY_ERROR", "An error occurred during API key generation"));
        }
    }

    // Request/Response DTOs

    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        private String merchantId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    }

    public static class LoginResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String userId;
        private final String username;
        private final String merchantId;
        private final Object roles;

        public LoginResponse(String accessToken, String refreshToken, String userId,
                             String username, String merchantId, Object roles) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.userId = userId;
            this.username = username;
            this.merchantId = merchantId;
            this.roles = roles;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getMerchantId() { return merchantId; }
        public Object getRoles() { return roles; }
    }

    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class RefreshTokenResponse {
        private final String accessToken;

        public RefreshTokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getAccessToken() { return accessToken; }
    }

    public static class ApiKeyResponse {
        private final String apiKey;

        public ApiKeyResponse(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiKey() { return apiKey; }
    }

    public static class ErrorResponse {
        private final String code;
        private final String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
