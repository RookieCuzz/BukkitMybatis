package com.cuzz.bukkitmybatis.logging;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

public final class LogSinks {
    private static final Object LOCK = new Object();
    private static volatile boolean initialized = false;
    private static volatile LogLevel level = LogLevel.INFO;
    private static volatile List<LogSink> sinks = Collections.emptyList();
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private LogSinks() {
    }

    public static void init(Plugin plugin, Properties xmlProperties) {
        if (initialized) {
            return;
        }
        synchronized (LOCK) {
            if (initialized) {
                return;
            }

            String levelValue = resolveConfigValue(xmlProperties, "log.level", "BUKKIT_MYBATIS_LOG_LEVEL");
            level = LogLevel.fromEnv(levelValue, LogLevel.INFO);
            String dirValue = resolveConfigValue(xmlProperties, "log.dir", "BUKKIT_MYBATIS_LOG_DIR");
            File baseDir = resolveBaseDir(plugin, dirValue);
            LogSinkConfig config = new LogSinkConfig(plugin.getName(), baseDir, System.getenv(), toMap(xmlProperties), plugin.getLogger());

            List<LogSink> loaded = new ArrayList<>();
            ServiceLoader<LogSink> loader = ServiceLoader.load(LogSink.class, plugin.getClass().getClassLoader());
            for (LogSink sink : loader) {
                try {
                    sink.init(config);
                    loaded.add(sink);
                } catch (Exception ex) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to init log sink: " + sink.getClass().getName(), ex);
                }
            }

            if (loaded.isEmpty()) {
                FileLogSink fallback = new FileLogSink();
                try {
                    fallback.init(config);
                    loaded.add(fallback);
                } catch (Exception ex) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to init fallback log sink.", ex);
                }
            }

            sinks = Collections.unmodifiableList(loaded);
            initialized = true;
        }
    }

    public static boolean isEnabled(LogLevel logLevel) {
        return level.allows(logLevel);
    }

    public static void log(LogLevel logLevel, String loggerName, String message, Throwable throwable) {
        if (!isEnabled(logLevel)) {
            return;
        }
        String enrichedMessage = appendContext(message);
        LogEvent event = new LogEvent(System.currentTimeMillis(), logLevel, loggerName, enrichedMessage, throwable);
        for (LogSink sink : sinks) {
            try {
                sink.log(event);
            } catch (Exception ignored) {
            }
        }
    }

    public static void shutdown() {
        for (LogSink sink : sinks) {
            try {
                sink.shutdown();
            } catch (Exception ignored) {
            }
        }
        sinks = Collections.emptyList();
        initialized = false;
    }

    private static File resolveBaseDir(Plugin plugin) {
        String overrideDir = System.getenv("BUKKIT_MYBATIS_LOG_DIR");
        File baseDir;
        if (overrideDir != null && !overrideDir.trim().isEmpty()) {
            baseDir = new File(overrideDir.trim());
        } else {
            baseDir = new File(plugin.getDataFolder(), "mybatis");
        }
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        return baseDir;
    }

    private static File resolveBaseDir(Plugin plugin, String overrideDir) {
        if (overrideDir != null && !overrideDir.trim().isEmpty()) {
            File baseDir = new File(overrideDir.trim());
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            return baseDir;
        }
        return resolveBaseDir(plugin);
    }

    private static String resolveConfigValue(Properties properties, String propertyKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        if (properties == null) {
            return null;
        }
        String value = properties.getProperty(propertyKey);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    private static HashMap<String, String> toMap(Properties properties) {
        HashMap<String, String> map = new HashMap<>();
        if (properties == null) {
            return map;
        }
        for (String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
        return map;
    }

    public static void setContext(String value) {
        if (value == null || value.trim().isEmpty()) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(value.trim());
        }
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

    private static String appendContext(String message) {
        String ctx = CONTEXT.get();
        if (ctx == null || ctx.isEmpty()) {
            return message;
        }
        if (message == null || message.isEmpty()) {
            return ctx;
        }
        return message + " " + ctx;
    }
}
