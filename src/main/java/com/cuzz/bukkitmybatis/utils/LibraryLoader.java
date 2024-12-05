package com.cuzz.bukkitmybatis.utils;

import com.cuzz.bukkitmybatis.BukkitMybatis;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
public final class LibraryLoader {

    private static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
    // 下载并将依赖添加到类路径中
    public static void downloadAndLoadDependency(String groupId, String artifactId, String version, String targetDirectory) {
        // 格式化文件路径
        String fileName = artifactId + "-" + version + ".jar";
        File targetFile = new File(targetDirectory, fileName);

        // 检查文件是否已经存在
        if (!targetFile.exists()) {
            // 下载文件
            downloadDependency(groupId, artifactId, version, targetDirectory);
        }

        // 加载该 jar 文件到类路径
        if (targetFile.exists()) {
            try {
                addJarToClasspath(targetFile);
                System.out.println("Dependency '" + artifactId + "-" + version + "' loaded successfully into the classpath.");
            } catch (Exception e) {
                System.err.println("Failed to load dependency: " + targetFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    // 下载依赖
    private static void downloadDependency(String groupId, String artifactId, String version, String targetDirectory) {
        // 格式化 URL
        String urlString = buildMavenUrl(groupId, artifactId, version);
        File targetFile = new File(targetDirectory, artifactId + "-" + version + ".jar");

        try {
            System.out.println("Downloading dependency from: " + urlString);

            // 创建 URL 对象
            URL url = new URL(urlString);
            try (InputStream in = url.openStream()) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Dependency downloaded to: " + targetFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Failed to download dependency: " + artifactId + "-" + version);
            e.printStackTrace();
        }
    }

    // 构建 Maven 中央仓库的 URL
    private static String buildMavenUrl(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace(".", "/");
        return MAVEN_CENTRAL_URL + groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    }

    // 将 jar 文件添加到类路径
    private static void addJarToClasspath(File jarFile) throws Exception {
        MavenDependencyLoader.loadJar(jarFile.toURL());
    }

    // 测试方法
    public static   void downLoadStart() {
        final File pluginDataFolder = BukkitMybatis.getInstance().getDataFolder();
        final File serverDir = pluginDataFolder.getParentFile().getParentFile();
        final File targetDirectory = new File(serverDir, "libraries");
        // HikariCP 依赖
        downloadAndLoadDependency("com.zaxxer", "HikariCP", "4.0.3", targetDirectory.getAbsolutePath());

        // MyBatis 依赖
        downloadAndLoadDependency("org.mybatis", "mybatis", "3.5.6", targetDirectory.getAbsolutePath());

        // MySQL 连接器依赖
        downloadAndLoadDependency("mysql", "mysql-connector-java", "8.0.23", targetDirectory.getAbsolutePath());
    }
}
