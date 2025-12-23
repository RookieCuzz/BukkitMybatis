package com.cuzz.bukkitmybatis.mybatis;

import com.cuzz.bukkitmybatis.logging.LogLevel;
import com.cuzz.bukkitmybatis.logging.LogSinks;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public final class SlowSqlInterceptor implements Interceptor {

    private static final String LOGGER_NAME = "SlowSql";
    private long thresholdMs = 200L;
    private LogLevel level = LogLevel.WARN;
    private int maxSqlLength = 2000;
    private boolean logParams = false;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        String context = buildTransactionContext(invocation.getTarget());
        LogSinks.setContext(context);
        try {
            return invocation.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            boolean debugEnabled = LogSinks.isEnabled(LogLevel.DEBUG);
            boolean infoEnabled = LogSinks.isEnabled(LogLevel.INFO);
            boolean isSlow = elapsedMs >= thresholdMs;
            try {
                Object[] args = invocation.getArgs();
                if (args != null && args.length >= 2 && args[0] instanceof MappedStatement) {
                    LogLevel emitLevel = debugEnabled ? LogLevel.DEBUG : (infoEnabled ? LogLevel.INFO : level);
                    if (debugEnabled || isSlow) {
                        if (LogSinks.isEnabled(emitLevel)) {
                            MappedStatement mappedStatement = (MappedStatement) args[0];
                            Object parameter = args[1];
                            BoundSql boundSql = resolveBoundSql(mappedStatement, parameter, args);
                            String sql = boundSql == null ? "unknown" : normalizeSql(boundSql.getSql());
                            StringBuilder message = new StringBuilder(256);
                            message.append("Slow SQL (").append(elapsedMs).append(" ms)");
                            if (isSlow) {
                                message.append(" slow=true");
                            }
                            message.append(" ")
                                    .append("id=").append(mappedStatement.getId())
                                    .append(" type=").append(mappedStatement.getSqlCommandType())
                                    .append(" sql=").append(truncate(sql, maxSqlLength));
                            if (logParams && boundSql != null) {
                                Object paramObj = boundSql.getParameterObject();
                                message.append(" params=").append(paramObj == null ? "null" : paramObj);
                            }
                            LogSinks.log(emitLevel, LOGGER_NAME, message.toString(), null);
                        }
                    }
                }
            } finally {
                LogSinks.clearContext();
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        if (properties == null) {
            return;
        }
        thresholdMs = parseLong(properties.getProperty("thresholdMs"), thresholdMs);
        level = LogLevel.fromEnv(properties.getProperty("level"), level);
        maxSqlLength = parseInt(properties.getProperty("maxSqlLength"), maxSqlLength);
        logParams = parseBoolean(properties.getProperty("logParams"), logParams);
    }

    private static BoundSql resolveBoundSql(MappedStatement mappedStatement, Object parameter, Object[] args) {
        if (args.length >= 6 && args[5] instanceof BoundSql) {
            return (BoundSql) args[5];
        }
        return mappedStatement.getBoundSql(parameter);
    }

    private static String normalizeSql(String sql) {
        if (sql == null) {
            return "unknown";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static String buildTransactionContext(Object target) {
        if (!(target instanceof Executor)) {
            return null;
        }
        Object transaction = extractTransaction((Executor) target);
        if (transaction == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(64);
        sb.append("tx=").append(transaction.getClass().getSimpleName())
                .append(":").append(Integer.toHexString(System.identityHashCode(transaction)));
        try {
            java.lang.reflect.Method method = transaction.getClass().getMethod("getConnection");
            Object connectionObj = method.invoke(transaction);
            if (connectionObj instanceof java.sql.Connection connection) {
                sb.append(" conn=").append(Integer.toHexString(System.identityHashCode(connection)));
                try {
                    sb.append(" autoCommit=").append(connection.getAutoCommit());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private static Object extractTransaction(Executor executor) {
        try {
            Class<?> baseExecutor = Class.forName("org.apache.ibatis.executor.BaseExecutor");
            if (!baseExecutor.isInstance(executor)) {
                return null;
            }
            try {
                java.lang.reflect.Method method = baseExecutor.getDeclaredMethod("getTransaction");
                method.setAccessible(true);
                return method.invoke(executor);
            } catch (NoSuchMethodException ignored) {
                java.lang.reflect.Field field = baseExecutor.getDeclaredField("transaction");
                field.setAccessible(true);
                return field.get(executor);
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
