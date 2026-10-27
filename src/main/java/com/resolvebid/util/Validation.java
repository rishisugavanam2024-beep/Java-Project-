package com.resolvebid.util;

import java.math.BigDecimal;
import com.resolvebid.exception.ValidationException;

public final class Validation {
    private Validation() { }

    public static String required(String value, String field) throws ValidationException {
        if (value == null || value.isBlank()) throw new ValidationException(field + " is required.");
        return value.trim();
    }

    public static BigDecimal positiveMoney(String value, String field) throws ValidationException {
        try {
            BigDecimal amount = new BigDecimal(required(value, field));
            if (amount.signum() <= 0) throw new ValidationException(field + " must be greater than zero.");
            return amount;
        } catch (NumberFormatException ex) {
            throw new ValidationException(field + " must be a valid number.");
        }
    }

    public static int positiveInt(String value, String field) throws ValidationException {
        try {
            int number = Integer.parseInt(required(value, field));
            if (number <= 0) throw new ValidationException(field + " must be greater than zero.");
            return number;
        } catch (NumberFormatException ex) {
            throw new ValidationException(field + " must be a whole number.");
        }
    }
}
