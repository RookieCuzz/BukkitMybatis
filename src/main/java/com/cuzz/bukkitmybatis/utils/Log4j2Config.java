package com.cuzz.bukkitmybatis.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log4j2Config {

    public static void setUp() {
        // 配置 Log4j2
        configureLog4j2();
    }

    private static void configureLog4j2() {
        // 获取 Log4j2 的 LoggerContext
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        // 创建日志格式（Pattern Layout）
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level: %msg%n%throwable")
                .build();

        // MyBatis 文件输出 Appender
        FileAppender fileAppender = FileAppender.newBuilder()
                .setName("MyBatisFileAppender")
                .withFileName("mybatis/mybatis.log") // 这个路径需要确保有效
                .setLayout(layout)
                .build();
        fileAppender.start();
        config.addAppender(fileAppender);
        // 创建 AsyncAppender，并将 FileAppender 作为目标
        AppenderRef appenderRef = AppenderRef.createAppenderRef("MyBatisFileAppender", null, null);
        AppenderRef[] appenderRefs = new AppenderRef[1];
        appenderRefs[0] = appenderRef;

        AsyncAppender asyncAppender = AsyncAppender.newBuilder()
                .setName("AsyncFile")
                .setAppenderRefs(appenderRefs) // 将 FileAppender 引用作为 AsyncAppender 的目标
                .setBlocking(false) // 设置是否阻塞
                .setConfiguration(config)
                .build();
        asyncAppender.start();

        config.addAppender(asyncAppender);
        // 设置 org.apache.ibatis 的日志记录器，级别为 trace，并且不继承父记录器
        LoggerConfig mybatisLoggerConfig = new LoggerConfig("org.apache.ibatis", Level.TRACE, false);
        mybatisLoggerConfig.addAppender(asyncAppender, Level.TRACE, null); // 使用 asyncAppender
        config.addLogger("org.apache.ibatis", mybatisLoggerConfig);

        // 更新 Log4j2 配置，使之生效
        context.updateLoggers();

        // 测试日志
        Logger logger = LogManager.getLogger("org.apache.ibatis");
        logger.trace("HI WORLD");
    }
}


