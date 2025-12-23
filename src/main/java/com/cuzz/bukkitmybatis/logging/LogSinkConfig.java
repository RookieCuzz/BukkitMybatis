package com.cuzz.bukkitmybatis.logging;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

public final class LogSinkConfig {
    private final String pluginName;
    private final File baseDir;
    private final Map<String, String> env;
    private final Map<String, String> properties;
    private final Logger logger;

    public LogSinkConfig(String pluginName, File baseDir, Map<String, String> env, Map<String, String> properties, Logger logger) {
        this.pluginName = pluginName;
        this.baseDir = baseDir;
        this.env = Collections.unmodifiableMap(env);
        this.properties = Collections.unmodifiableMap(properties);
        this.logger = logger;
    }

    public String getPluginName() {
        return pluginName;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public Logger getLogger() {
        return logger;
    }

    public String env(String key) {
        return env.get(key);
    }

    public String property(String key) {
        return properties.get(key);
    }

    public String propertyOrEnv(String propertyKey, String envKey) {
        String envValue = env(envKey);
        if (!isBlank(envValue)) {
            return envValue;
        }
        return property(propertyKey);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
