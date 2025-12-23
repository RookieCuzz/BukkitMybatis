package com.cuzz.bukkitmybatis.service;

import com.cuzz.bukkitmybatis.api.MybatisService;
import com.cuzz.bukkitmybatis.utils.MapperRegister;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MybatisServiceImpl implements MybatisService {
    private final Plugin owner;
    private final SqlSessionFactory sqlSessionFactory;

    public MybatisServiceImpl(Plugin owner, SqlSessionFactory sqlSessionFactory) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.sqlSessionFactory = Objects.requireNonNull(sqlSessionFactory, "sqlSessionFactory");
    }

    @Override
    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    @Override
    public <T> T withSession(Function<SqlSession, T> work) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return work.apply(session);
        }
    }

    @Override
    public void withSession(Consumer<SqlSession> work) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            work.accept(session);
        }
    }

    @Override
    public void registerMapper(Plugin plugin, String resourcePath) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null.");
        }
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be empty.");
        }
        try (var input = plugin.getResource(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Mapper resource not found: " + resourcePath);
            }
            MapperRegister.registerMapper(resourcePath, input);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to register mapper: " + resourcePath, ex);
        }
    }

    @Override
    public void registerMapper(String pluginPackage, String resourcePath) {
        Plugin target = findPluginByPackage(pluginPackage);
        if (target == null) {
            throw new IllegalArgumentException("Plugin not found for package: " + pluginPackage);
        }
        registerMapper(target, resourcePath);
    }

    private Plugin findPluginByPackage(String pluginPackage) {
        if (pluginPackage == null || pluginPackage.trim().isEmpty()) {
            return null;
        }
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            String pkg = plugin.getClass().getPackageName();
            if (pkg != null && pkg.startsWith(pluginPackage)) {
                return plugin;
            }
        }
        return null;
    }
}
