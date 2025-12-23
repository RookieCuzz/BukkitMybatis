package com.cuzz.bukkitmybatis.logging;

import org.apache.ibatis.logging.Log;

public final class BukkitMybatisLog implements Log {
    private final String loggerName;

    public BukkitMybatisLog(String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public boolean isDebugEnabled() {
        return LogSinks.isEnabled(LogLevel.DEBUG);
    }

    @Override
    public boolean isTraceEnabled() {
        return LogSinks.isEnabled(LogLevel.TRACE);
    }

    @Override
    public void error(String s, Throwable e) {
        LogSinks.log(LogLevel.ERROR, loggerName, s, e);
    }

    @Override
    public void error(String s) {
        LogSinks.log(LogLevel.ERROR, loggerName, s, null);
    }

    @Override
    public void debug(String s) {
        LogSinks.log(LogLevel.DEBUG, loggerName, s, null);
    }

    @Override
    public void trace(String s) {
        LogSinks.log(LogLevel.TRACE, loggerName, s, null);
    }

    @Override
    public void warn(String s) {
        LogSinks.log(LogLevel.WARN, loggerName, s, null);
    }
}
