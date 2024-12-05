package com.cuzz.bukkitmybatis.utils;

import java.net.URL;
import java.net.URLClassLoader;

public class MavenDependencyLoader {

    public static void loadJar(URL jarUrl) {
        try {
            // 获取当前类加载器
            ClassLoader classLoader = MavenDependencyLoader.class.getClassLoader();

            // 判断是否是 URLClassLoader 实例
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;

                // 使用反射调用 addURL 方法
                URLClassLoaderAccess access = URLClassLoaderAccess.create(urlClassLoader);
                access.addURL(jarUrl);

                System.out.println("Jar loaded successfully: " + jarUrl);
            } else {
                System.out.println("ClassLoader is not an instance of URLClassLoader");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load JAR file", e);
        }
    }

}
