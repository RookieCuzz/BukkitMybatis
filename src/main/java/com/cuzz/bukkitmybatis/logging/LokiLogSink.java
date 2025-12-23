package com.cuzz.bukkitmybatis.logging;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class LokiLogSink implements LogSink {
    private static final String LOKI_PUSH_PATH = "/loki/api/v1/push";

    private boolean enabled;
    private String endpoint;
    private String tenant;
    private String authHeader;
    private Map<String, String> labels;
    private ExecutorService executor;
    private Logger logger;
    private boolean debugEnabled;
    private Path debugLog;
    private final Object debugLock = new Object();

    @Override
    public void init(LogSinkConfig config) {
        String url = config.propertyOrEnv("loki.url", "BUKKIT_MYBATIS_LOKI_URL");
        if (isBlank(url)) {
            enabled = false;
            return;
        }

        endpoint = url.endsWith(LOKI_PUSH_PATH) ? url : url + LOKI_PUSH_PATH;
        tenant = config.propertyOrEnv("loki.tenant", "BUKKIT_MYBATIS_LOKI_TENANT");
        String username = config.propertyOrEnv("loki.username", "BUKKIT_MYBATIS_LOKI_USERNAME");
        String password = config.propertyOrEnv("loki.password", "BUKKIT_MYBATIS_LOKI_PASSWORD");
        authHeader = buildAuthHeader(username, password);
        labels = parseLabels(config.propertyOrEnv("loki.labels", "BUKKIT_MYBATIS_LOKI_LABELS"));
        labels.putIfAbsent("app", config.getPluginName());
        logger = config.getLogger();
        debugEnabled = isTrue(config.propertyOrEnv("loki.debug", "BUKKIT_MYBATIS_LOKI_DEBUG"));
        if (debugEnabled) {
            debugLog = config.getBaseDir().toPath().resolve("loki-debug.log");
            try {
                Files.createDirectories(debugLog.getParent());
                Files.writeString(debugLog, "", StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                debug("Loki debug enabled. endpoint=" + endpoint + ", tenant=" + safe(tenant) + ", labels=" + labels);
            } catch (Exception ex) {
                debugEnabled = false;
            }
        }

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "bukkitmybatis-loki");
            t.setDaemon(true);
            return t;
        });
        enabled = true;
    }

    @Override
    public void log(LogEvent event) {
        if (!enabled || executor == null) {
            return;
        }
        executor.execute(() -> send(event));
    }

    @Override
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void send(LogEvent event) {
        try {
            String payload = buildPayload(event);
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            if (tenant != null && !tenant.isEmpty()) {
                conn.setRequestProperty("X-Scope-OrgID", tenant);
            }
            if (authHeader != null) {
                conn.setRequestProperty("Authorization", authHeader);
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            int code = conn.getResponseCode();
            String response = readBody(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (debugEnabled) {
                debug("Loki push status=" + code + " body=" + safe(response));
                if (logger != null) {
                    logger.info("Loki push status=" + code + " body=" + safe(response));
                }
            }
        } catch (Exception ignored) {
            if (debugEnabled) {
                debug("Loki push failed: " + ignored.getClass().getName() + " " + safe(ignored.getMessage()));
                if (logger != null) {
                    logger.warning("Loki push failed: " + ignored.getClass().getName() + " " + safe(ignored.getMessage()));
                }
            }
        }
    }

    private String buildPayload(LogEvent event) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> streamLabels = buildStreamLabels(event);
        sb.append("{\"streams\":[{\"stream\":{");
        boolean first = true;
        for (Map.Entry<String, String> entry : streamLabels.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                    .append(escapeJson(entry.getValue())).append("\"");
        }
        sb.append("},\"values\":[[");
        sb.append("\"").append(event.getTimestampMillis() * 1_000_000L).append("\",");
        sb.append("\"").append(escapeJson(formatMessage(event))).append("\"");
        sb.append("]]}]}");
        return sb.toString();
    }

    private String formatMessage(LogEvent event) {
        String base = "[" + event.getLevel() + "] " + event.getLoggerName() + " - " + event.getMessage();
        if (event.getThrowable() == null) {
            return base;
        }
        return base + " | " + event.getThrowable().toString();
    }

    private Map<String, String> buildStreamLabels(LogEvent event) {
        Map<String, String> streamLabels = new HashMap<>(labels);
        if (event != null && event.getMessage() != null && event.getMessage().contains("slow=true")) {
            streamLabels.put("slow", "true");
        }
        return streamLabels;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static Map<String, String> parseLabels(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            return map;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private static String buildAuthHeader(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return null;
        }
        String token = username + ":" + password;
        String encoded = java.util.Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private static String readBody(java.io.InputStream stream) {
        if (stream == null) {
            return "";
        }
        try (Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private void debug(String message) {
        if (!debugEnabled || debugLog == null) {
            return;
        }
        String line = Instant.now().toString() + " " + message + System.lineSeparator();
        synchronized (debugLock) {
            try {
                Files.writeString(debugLog, line, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isTrue(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("on");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
