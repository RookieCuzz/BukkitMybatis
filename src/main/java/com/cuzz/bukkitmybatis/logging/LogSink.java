package com.cuzz.bukkitmybatis.logging;

public interface LogSink {
    void init(LogSinkConfig config);
    void log(LogEvent event);
    void shutdown();
}
