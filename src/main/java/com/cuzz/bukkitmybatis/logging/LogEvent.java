package com.cuzz.bukkitmybatis.logging;

public final class LogEvent {
    private final long timestampMillis;
    private final LogLevel level;
    private final String loggerName;
    private final String message;
    private final Throwable throwable;

    public LogEvent(long timestampMillis, LogLevel level, String loggerName, String message, Throwable throwable) {
        this.timestampMillis = timestampMillis;
        this.level = level;
        this.loggerName = loggerName;
        this.message = message;
        this.throwable = throwable;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
