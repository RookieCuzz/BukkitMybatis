package com.cuzz.bukkitmybatis.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class FileLogSink implements LogSink {
    private final Object lock = new Object();
    private BufferedWriter writer;
    private Path logFile;
    private static final ZoneId LOG_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(LOG_ZONE);

    @Override
    public void init(LogSinkConfig config) {
        try {
            Files.createDirectories(config.getBaseDir().toPath());
            logFile = config.getBaseDir().toPath().resolve("mybatis.log");
            writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize file log sink", ex);
        }
    }

    @Override
    public void log(LogEvent event) {
        if (writer == null) {
            return;
        }
        String timestamp = LOG_FORMATTER.format(Instant.ofEpochMilli(event.getTimestampMillis()));
        String line = timestamp + " [" + event.getLevel() + "] " + event.getLoggerName() + " - " + event.getMessage();

        synchronized (lock) {
            try {
                writer.write(line);
                writer.newLine();
                if (event.getThrowable() != null) {
                    writer.write(formatThrowable(event.getThrowable()));
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void shutdown() {
        if (writer == null) {
            return;
        }
        synchronized (lock) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
            writer = null;
        }
    }

    private String formatThrowable(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString().trim();
    }
}
