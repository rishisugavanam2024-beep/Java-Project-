package com.resolvebid.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceRequest(long id, String customerName, String category, String description,
        String urgency, BigDecimal budget, RequestStatus status, LocalDateTime createdAt) {
    @Override public String toString() {
        return "#" + id + " · " + category + " · " + description;
    }
}
