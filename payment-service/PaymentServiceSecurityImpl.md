# Encryption Utilities

This package provides secure encryption and tokenization utilities for sensitive payment data, ensuring PCI DSS compliance.

## Components

### 1. EncryptionService
AES-256-GCM encryption service for sensitive data.

```java
@Autowired
private EncryptionService encryptionService;

// Encrypt sensitive data
String encrypted = encryptionService.encrypt("4111111111111111");

// Decrypt data
String decrypted = encryptionService.decrypt(encrypted);
```

### 2. EncryptedStringConverter
JPA attribute converter for automatic encryption/decryption of entity fields.

```java
@Entity
public class PaymentData {
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "card_number", length = 500)
    private String cardNumber; // Automatically encrypted in database
}
```

### 3. TokenizationService
Secure tokenization service for card numbers and sensitive payment data.

```java
@Autowired
private TokenizationService tokenizationService;

// Tokenize a card number
String token = tokenizationService.tokenize("4111111111111111");
// Returns: "tok_abc123def456..."

// Detokenize back to original
String cardNumber = tokenizationService.detokenize(token);

// Get masked version for display
String masked = tokenizationService.maskCardNumber("4111111111111111");
// Returns: "**** **** **** 1111"

// Check if string is a token
boolean isToken = tokenizationService.isToken("tok_abc123def456...");
```

## Configuration

Configure encryption key in `application.yml`:

```yaml
payment:
  security:
    encryption:
      key: ${ENCRYPTION_KEY:} # Base64 encoded AES-256 key
```

## Security Features

- **AES-256-GCM**: Authenticated encryption with integrity protection
- **Random IV**: Each encryption uses a unique initialization vector
- **Secure tokenization**: Card numbers are never stored in plain text
- **PCI DSS compliance**: Meets requirements for sensitive data protection
- **Key management**: Supports external key management systems (AWS KMS)

## Production Considerations

1. **Key Management**: Use AWS KMS or similar for encryption key storage
2. **Token Vault**: Replace in-memory token storage with secure vault
3. **Audit Logging**: Log all encryption/decryption operations
4. **Key Rotation**: Implement regular key rotation procedures
5. **Performance**: Consider caching for frequently accessed tokens

## Testing

All components include comprehensive unit tests covering:
- Encryption/decryption functionality
- Error handling and edge cases
- Token generation and validation
- JPA converter integration