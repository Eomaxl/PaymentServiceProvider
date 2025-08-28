package com.paymentprovider.paymentservice.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumeration of supported currencies with ISO 4217 codes
 */
public enum Currency {
    // Major currencies
    USD("USD", "US Dollar", 2),
    EUR("EUR", "Euro", 2),
    GBP("GBP", "British Pound", 2),
    JPY("JPY", "Japanese Yen", 0),
    CHF("CHF", "Swiss Franc", 2),
    CAD("CAD", "Canadian Dollar", 2),
    AUD("AUD", "Australian Dollar", 2),

    // Asian currencies
    CNY("CNY", "Chinese Yuan", 2),
    HKD("HKD", "Hong Kong Dollar", 2),
    SGD("SGD", "Singapore Dollar", 2),
    KRW("KRW", "South Korean Won", 0),
    INR("INR", "Indian Rupee", 2),
    THB("THB", "Thai Baht", 2),
    MYR("MYR", "Malaysian Ringgit", 2),

    // European currencies
    SEK("SEK", "Swedish Krona", 2),
    NOK("NOK", "Norwegian Krone", 2),
    DKK("DKK", "Danish Krone", 2),
    PLN("PLN", "Polish Zloty", 2),
    CZK("CZK", "Czech Koruna", 2),
    HUF("HUF", "Hungarian Forint", 2),

    // Latin American currencies
    BRL("BRL", "Brazilian Real", 2),
    MXN("MXN", "Mexican Peso", 2),
    ARS("ARS", "Argentine Peso", 2),
    CLP("CLP", "Chilean Peso", 0),

    // Middle Eastern currencies
    AED("AED", "UAE Dirham", 2),
    SAR("SAR", "Saudi Riyal", 2),
    ILS("ILS", "Israeli Shekel", 2),

    // African currencies
    ZAR("ZAR", "South African Rand", 2),
    EGP("EGP", "Egyptian Pound", 2),

    // Other currencies
    RUB("RUB", "Russian Ruble", 2),
    TRY("TRY", "Turkish Lira", 2),
    NZD("NZD", "New Zealand Dollar", 2);

    private final String code;
    private final String displayName;
    private final int decimalPlaces;

    Currency(String code, String displayName, int decimalPlaces) {
        this.code = code;
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public static Currency fromCode(String code) {
        for (Currency currency : values()) {
            if (currency.code.equalsIgnoreCase(code)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Unknown currency code: " + code);
    }

    public static Set<String> getAllCodes() {
        return Arrays.stream(values())
                .map(Currency::getCode)
                .collect(Collectors.toSet());
    }

    public boolean isZeroDecimal() {
        return decimalPlaces == 0;
    }
}
