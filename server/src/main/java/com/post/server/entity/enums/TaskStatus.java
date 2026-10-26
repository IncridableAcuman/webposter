package com.post.server.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    COMPLETE("complete"),
    INCOMPLETE("incomplete");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TaskStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return INCOMPLETE;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("complete") || normalized.equals("complate")) {
            return COMPLETE;
        }
        if (normalized.equals("incomplete") || normalized.equals("incomplate")) {
            return INCOMPLETE;
        }
        try {
            return TaskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown task status: " + value + ". Allowed values: complete, incomplete");
        }
    }
}
