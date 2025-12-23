package com.cuzz.bukkitmybatis.utils;

import com.cuzz.bukkitmybatis.BukkitMybatis;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

public final class VirtualDbExecutor {
    private static final ExecutorService EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("bukkitmybatis-db-", 0).factory()
    );

    private VirtualDbExecutor() {
    }

    public static void run(Consumer<SqlSession> task) {
        run(BukkitMybatis.getInstance().getSqlSessionFactory(), task);
    }

    public static void run(SqlSessionFactory factory, Consumer<SqlSession> task) {
        EXECUTOR.execute(() -> {
            try (SqlSession session = factory.openSession(true)) {
                task.accept(session);
            }
        });
    }

    public static <T> Future<T> submit(Function<SqlSession, T> task) {
        return submit(BukkitMybatis.getInstance().getSqlSessionFactory(), task);
    }

    public static <T> Future<T> submit(SqlSessionFactory factory, Function<SqlSession, T> task) {
        return EXECUTOR.submit(() -> {
            try (SqlSession session = factory.openSession(true)) {
                return task.apply(session);
            }
        });
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}
