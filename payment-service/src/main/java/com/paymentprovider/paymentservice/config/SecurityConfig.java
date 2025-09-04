package com.paymentprovider.paymentservice.config;

import com.paymentprovider.paymentservice.security.AuthenticationService;
import com.paymentprovider.paymentservice.security.JwtAuthenticationFilter;
import com.paymentprovider.paymentservice.security.ApiKeyAuthenticationFilter;
import com.paymentprovider.paymentservice.security.RateLimitingFilter;
import com.paymentprovider.paymentservice.security.SecurityEventFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the payment service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConfigurationProperties(prefix = "payment.security")
public class SecurityConfig {

    private final AuthenticationService authenticationService;
    private final RateLimitingFilter rateLimitingFilter;
    private final SecurityEventFilter securityEventFilter;

    private Encryption encryption = new Encryption();
    private Jwt jwt = new Jwt();

    @Autowired
    public SecurityConfig(AuthenticationService authenticationService,
                          RateLimitingFilter rateLimitingFilter,
                          SecurityEventFilter securityEventFilter) {
        this.authenticationService = authenticationService;
        this.rateLimitingFilter = rateLimitingFilter;
        this.securityEventFilter = securityEventFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        // Protected endpoints
                        .requestMatchers("/api/v1/payments/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityEventFilter, RateLimitingFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter(), JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(authenticationService);
    }

    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter() {
        return new ApiKeyAuthenticationFilter(authenticationService);
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public static class Encryption {
        /**
         * Base64 encoded AES-256 encryption key.
         * In production, this should be retrieved from AWS KMS or similar secure key management service.
         */
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMinutes = 15;
        private long refreshTokenExpirationDays = 7;
        private String issuer = "payment-service";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirationMinutes() {
            return accessTokenExpirationMinutes;
        }

        public void setAccessTokenExpirationMinutes(long accessTokenExpirationMinutes) {
            this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        }

        public long getRefreshTokenExpirationDays() {
            return refreshTokenExpirationDays;
        }

        public void setRefreshTokenExpirationDays(long refreshTokenExpirationDays) {
            this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }
}
