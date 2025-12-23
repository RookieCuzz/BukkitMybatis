package com.cuzz.bukkitmybatis.api;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.function.Consumer;
import java.util.function.Function;

public interface MybatisService {
    SqlSessionFactory getSqlSessionFactory();

    <T> T withSession(Function<SqlSession, T> work);

    void withSession(Consumer<SqlSession> work);

    void registerMapper(org.bukkit.plugin.Plugin plugin, String resourcePath);

    void registerMapper(String pluginPackage, String resourcePath);
}
