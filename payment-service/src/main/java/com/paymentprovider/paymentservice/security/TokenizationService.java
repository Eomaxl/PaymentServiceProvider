package com.paymentprovider.paymentservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for tokenizing sensitive payment card data.
 * Provides secure tokenization and detokenization of card numbers
 * while maintaining PCI DSS compliance by never storing raw PAN data.
 */
@Service
public class TokenizationService {

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{13,19}$");
    private static final String TOKEN_PREFIX = "tok_";
    private static final int TOKEN_LENGTH = 32;

    private final EncryptionService encryptionService;
    private final SecureRandom secureRandom;

    // In production, this should be replaced with a secure token vault (e.g., AWS Payment Cryptography)
    private final ConcurrentHashMap<String, String> tokenVault = new ConcurrentHashMap<>();

    @Autowired
    public TokenizationService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Tokenizes a card number, replacing it with a secure token.
     * The original card number is encrypted and stored securely.
     *
     * @param cardNumber the card number to tokenize (PAN)
     * @return secure token representing the card number
     * @throws TokenizationException if tokenization fails
     */
    public String tokenize(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new TokenizationException("Card number cannot be null or empty");
        }

        String cleanCardNumber = cardNumber.replaceAll("\\s+", "");

        if (!CARD_NUMBER_PATTERN.matcher(cleanCardNumber).matches()) {
            throw new TokenizationException("Invalid card number format");
        }

        try {
            // Generate a unique token
            String token = generateToken();

            // Encrypt and store the card number
            String encryptedCardNumber = encryptionService.encrypt(cleanCardNumber);
            tokenVault.put(token, encryptedCardNumber);

            return token;
        } catch (Exception e) {
            throw new TokenizationException("Failed to tokenize card number", e);
        }
    }

    /**
     * Detokenizes a token back to the original card number.
     *
     * @param token the token to detokenize
     * @return the original card number
     * @throws TokenizationException if detokenization fails
     */
    public String detokenize(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            throw new TokenizationException("Invalid token format");
        }

        String encryptedCardNumber = tokenVault.get(token);
        if (encryptedCardNumber == null) {
            throw new TokenizationException("Token not found");
        }

        try {
            return encryptionService.decrypt(encryptedCardNumber);
        } catch (Exception e) {
            throw new TokenizationException("Failed to detokenize token", e);
        }
    }

    /**
     * Checks if a given string is a valid token.
     *
     * @param token the string to check
     * @return true if the string is a valid token
     */
    public boolean isToken(String token) {
        return token != null &&
                token.startsWith(TOKEN_PREFIX) &&
                token.length() == TOKEN_PREFIX.length() + TOKEN_LENGTH &&
                tokenVault.containsKey(token);
    }

    /**
     * Gets the masked version of a card number for display purposes.
     * Shows only the last 4 digits with the rest masked.
     *
     * @param cardNumber the card number to mask
     * @return masked card number (e.g., "**** **** **** 1234")
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        String cleanCardNumber = cardNumber.replaceAll("\\s+", "");
        if (cleanCardNumber.length() < 4) {
            return "****";
        }

        String lastFour = cleanCardNumber.substring(cleanCardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    /**
     * Gets the masked version of a tokenized card number.
     *
     * @param token the token representing the card number
     * @return masked card number
     */
    public String getMaskedCardNumber(String token) {
        try {
            String cardNumber = detokenize(token);
            return maskCardNumber(cardNumber);
        } catch (TokenizationException e) {
            return "****";
        }
    }

    /**
     * Removes a token from the vault (for cleanup purposes).
     *
     * @param token the token to remove
     * @return true if the token was removed, false if it didn't exist
     */
    public boolean removeToken(String token) {
        return tokenVault.remove(token) != null;
    }

    private String generateToken() {
        try {
            // Generate random bytes for the token
            byte[] randomBytes = new byte[24]; // 24 bytes = 32 characters when base64 encoded
            secureRandom.nextBytes(randomBytes);

            // Create a hash to ensure uniqueness
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(randomBytes);

            // Take first 24 bytes of hash and encode as base64
            byte[] tokenBytes = new byte[24];
            System.arraycopy(hash, 0, tokenBytes, 0, 24);

            String tokenSuffix = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(tokenBytes);

            return TOKEN_PREFIX + tokenSuffix;
        } catch (NoSuchAlgorithmException e) {
            throw new TokenizationException("Failed to generate token", e);
        }
    }
}
