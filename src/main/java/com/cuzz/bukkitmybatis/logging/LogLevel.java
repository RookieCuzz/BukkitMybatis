package com.cuzz.bukkitmybatis.logging;

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    OFF;

    public static LogLevel fromEnv(String value, LogLevel fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return LogLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public boolean allows(LogLevel level) {
        if (this == OFF) {
            return false;
        }
        return level.ordinal() >= this.ordinal();
    }
}
