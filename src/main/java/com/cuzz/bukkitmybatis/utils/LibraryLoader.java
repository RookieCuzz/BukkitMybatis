package com.cuzz.bukkitmybatis.utils;

import com.cuzz.bukkitmybatis.BukkitMybatis;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class LibraryLoader {

    private static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";

    public static boolean downloadAndLoadDependency(String groupId, String artifactId, String version, String targetDirectory) {
        String fileName = artifactId + "-" + version + ".jar";
        File targetFile = new File(targetDirectory, fileName);

        if (!targetFile.exists()) {
            if (!downloadDependency(groupId, artifactId, version, targetDirectory)) {
                return false;
            }
        }

        if (!targetFile.exists()) {
            return false;
        }

        try {
            addJarToClasspath(targetFile);
            System.out.println("Dependency '" + artifactId + "-" + version + "' loaded successfully into the classpath.");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load dependency: " + targetFile.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean downloadDependency(String groupId, String artifactId, String version, String targetDirectory) {
        String urlString = buildMavenUrl(groupId, artifactId, version);
        File targetFile = new File(targetDirectory, artifactId + "-" + version + ".jar");

        try {
            System.out.println("Downloading dependency from: " + urlString);
            URL url = new URL(urlString);
            try (InputStream in = url.openStream()) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Dependency downloaded to: " + targetFile.getAbsolutePath());
            }
            return true;
        } catch (Exception e) {
            System.err.println("Failed to download dependency: " + artifactId + "-" + version);
            e.printStackTrace();
            return false;
        }
    }

    private static String buildMavenUrl(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace(".", "/");
        return MAVEN_CENTRAL_URL + groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    }

    private static void addJarToClasspath(File jarFile) throws Exception {
        MavenDependencyLoader.loadJar(jarFile.toURL());
    }

    public static boolean downLoadStart() {
        File pluginDataFolder = BukkitMybatis.getInstance().getDataFolder();
        File serverDir = pluginDataFolder.getParentFile().getParentFile();
        File targetDirectory = new File(serverDir, "libraries");

        try {
            Files.createDirectories(targetDirectory.toPath());
        } catch (Exception e) {
            System.err.println("Failed to create libraries directory: " + targetDirectory.getAbsolutePath());
            e.printStackTrace();
            return false;
        }

        boolean ok = true;
        ok &= downloadAndLoadDependency("com.zaxxer", "HikariCP", "4.0.3", targetDirectory.getAbsolutePath());
        ok &= downloadAndLoadDependency("org.mybatis", "mybatis", "3.5.6", targetDirectory.getAbsolutePath());
        ok &= downloadAndLoadDependency("mysql", "mysql-connector-java", "8.0.23", targetDirectory.getAbsolutePath());
        return ok;
    }
}
