package com.cuzz.bukkitmybatis.utils;

import com.cuzz.bukkitmybatis.BukkitMybatis;
import com.cuzz.bukkitmybatis.mapper.TestMapper2;
import com.cuzz.bukkitmybatis.model.Group;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class MapperRegister {

    /**
     *  注册单个个mapper.xml
     * @param mapperFile  对应的file
     */

    // 注册单个映射器
    public static void registerMapper(File mapperFile) {
        Configuration configuration = BukkitMybatis.instance.getSqlSessionFactory().getConfiguration();
        try (FileInputStream mapperXmlStream = new FileInputStream(mapperFile)) {
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                    mapperXmlStream,
                    configuration,
                    mapperFile.getAbsolutePath(),
                    configuration.getSqlFragments()
            );
            xmlMapperBuilder.parse();

            BukkitMybatis.instance.getLogger().log(Level.INFO, "成功注册: " + mapperFile.getAbsolutePath());
        } catch (IOException e) {
            // 捕获 I/O 异常，并记录日志
                e.printStackTrace();
        }
    }

    /**
     *  卸载某个插件注册的mapper
     * @param plugin
     */
    public synchronized static void unregisterMapper(JavaPlugin plugin) {

        Configuration configuration = BukkitMybatis.instance.getSqlSessionFactory().getConfiguration();
        String packageName = plugin.getClass().getPackageName();
        try {
            //清除MappedStatement
            Collection<MappedStatement> mappedStatements = configuration.getMappedStatements();
            Iterator<MappedStatement> mappedStatementIterator = mappedStatements.iterator();
            while (mappedStatementIterator.hasNext()) {
                MappedStatement mappedStatement = mappedStatementIterator.next();
                if (mappedStatement.getId().startsWith(packageName)) {
                    mappedStatementIterator.remove();
                    System.out.println("清除 mappedStatement " +mappedStatement.getId());
                }
            }

            // 清除自定义 TypeHandler
            Collection<TypeHandler<?>> typeHandlers = configuration.getTypeHandlerRegistry().getTypeHandlers();
            Iterator<TypeHandler<?>> typeHandlersIterator = typeHandlers.iterator();
            while (typeHandlersIterator.hasNext()) {
                TypeHandler<?> typeHandler = typeHandlersIterator.next();
                if (typeHandler.getClass().getName().startsWith(packageName)) {
                    typeHandlersIterator.remove();
                    System.out.println("清除 TypeHandler " + typeHandler.getClass().getName());
                }
            }


            // 清除拦截器
            List<Interceptor> interceptors = configuration.getInterceptors();
            Iterator<Interceptor> interceptorsIterator = interceptors.iterator();
            while (interceptorsIterator.hasNext()) {
                Interceptor interceptor = interceptorsIterator.next();
                if (interceptor.getClass().getName().startsWith(packageName)) {
                    interceptorsIterator.remove();
                    System.out.println("清除 Interceptor " + interceptor.getClass().getName());
                }
            }

            //清除二级缓存
            Collection<Cache> caches = configuration.getCaches();
            Iterator<Cache> cachesIterator = caches.iterator();
            while (cachesIterator.hasNext()) {
                Cache next = cachesIterator.next();
                if (next.getId().startsWith(packageName)) {
                    cachesIterator.remove(); // 安全地移除元素
                    System.out.println("清除二级缓存映射"+next.getId());
                }
            }

            //清除已加载的资源映射
                Field loadedResourcesField = Configuration.class.getDeclaredField("loadedResources");
                loadedResourcesField.setAccessible(true); // 绕过访问限制
                Set<String> resources = (Set<String>) loadedResourcesField.get(configuration);
                System.out.println("清除已经加载的资源"+resources);
                resources.clear();
                //清除结果映射
                Collection<ResultMap> resultMaps = configuration.getResultMaps();
                Iterator<ResultMap> resultMapsIterator = resultMaps.iterator();
                while (resultMapsIterator.hasNext()) {
                    ResultMap resultMap = resultMapsIterator.next();
                    if (resultMap.getId().startsWith(packageName)) {
                        resultMapsIterator.remove(); // 安全地移除元素
                        System.out.println("清除结果映射"+resultMap.getId());
                    }
                 }
                //清除参数映射
                Collection<ParameterMap> parameterMaps = configuration.getParameterMaps();
                Iterator<ParameterMap> ParameterMapIterator = parameterMaps.iterator();
                while (ParameterMapIterator.hasNext()) {
                    ParameterMap parameterMap = ParameterMapIterator.next();
                    if (parameterMap.getId().startsWith(packageName)) {
                        ParameterMapIterator.remove(); // 安全地移除元素
                        System.out.println("清除参数映射"+parameterMap.getId());
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     *
     * @param plugin 插件
     */
    public static void registerMappers(JavaPlugin plugin) {
        int successfulCount = 0;
        int failedCount = 0;
        Set<File> mapperFiles= savePluginAllMapperXML(plugin,false);
        for (File file : mapperFiles) {
            try {
                registerMapper(file);
                successfulCount++;
            } catch (Exception e) {
                failedCount++;
                BukkitMybatis.instance.getLogger().log(Level.WARNING, "无法注册: " + file.getAbsolutePath(), e);
            }
        }

        BukkitMybatis.instance.getLogger().log(Level.INFO,
                "\u001B[32m" + plugin.getName() + " >> 映射器注册完成: 成功 " + successfulCount + "，失败 " + failedCount + "\u001B[0m"
        );

    }


    /**
     *
     * @param plugin
     * @return 获取插件的JARURL路径
     */
    public static URL getPluginJar(Plugin plugin) {

        try {
            // 获取当前类所属的 JAR 文件
            Class<?> clazz = plugin.getClass(); // 也可以替换成你需要的任何类
            URL location = clazz.getProtectionDomain().getCodeSource().getLocation();

            // 如果是 JAR 文件，路径应该以 .jar 结尾
            if (location.getPath().endsWith(".jar")) {
                // 这是 JAR 文件路径
                return location;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     *
     * @param plugin
     * @return  保存(强行覆盖)并返回插件的所有xml文件路径
     */

    public static Set<File> savePluginAllMapperXML(JavaPlugin plugin, Boolean replace) {
        HashSet<File> files = new HashSet<>();
        URL pluginJar = getPluginJar(plugin);
        File outputDirectory = new File(String.valueOf(plugin.getDataFolder()));

        if (pluginJar != null) {
            try {
                // 打开 JAR 文件
                JarFile jarFile = new JarFile(pluginJar.getFile());

                // 获取 JAR 文件中的所有条目
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // 如果文件在 "mappers/" 文件夹下并且以 .xml 结尾
                    if (entryName.startsWith("mappers/") && entryName.endsWith(".xml")) {
                        // 计算输出文件路径
                        Path outputPath = outputDirectory.toPath().resolve(entryName);

                        System.out.println(outputPath);
                        // 如果文件是目录则创建它
                        if (entry.isDirectory()) {
                            Files.createDirectories(outputPath);
                        } else {
                            // 检查文件是否已存在，并根据 replace 参数选择是否覆盖
                            if (Files.exists(outputPath) && !replace) {
                                // 如果文件已存在且 replace 为 false，则跳过
                                // 将保存到本地的文件加入到结果集合中
                                files.add(outputPath.toFile());
                                continue;
                            }

                            // 确保父目录存在
                            Files.createDirectories(outputPath.getParent());

                            // 如果是文件则提取并保存到本地
                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                System.out.println("copy 文件啦");
                                if (replace) {
                                    // 如果 replace 为 true，使用 REPLACE_EXISTING 覆盖文件
                                    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                                } else {
                                    // 如果 replace 为 false，则不覆盖已存在的文件
                                    Files.copy(inputStream, outputPath);
                                }

                                // 将保存到本地的文件加入到结果集合中
                                files.add(outputPath.toFile());
                                System.out.println("FILES 大小"+files.size());
                            }
                        }


                    }
                }

                // 关闭 JAR 文件
                jarFile.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return files;
    }



}
