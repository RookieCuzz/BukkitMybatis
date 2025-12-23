package com.cuzz.bukkitmybatis;

import com.cuzz.bukkitmybatis.api.MybatisService;
import com.cuzz.bukkitmybatis.mapper.TestMapper2;
import com.cuzz.bukkitmybatis.utils.LibraryLoader;
import com.cuzz.bukkitmybatis.service.MybatisServiceImpl;
import com.cuzz.bukkitmybatis.utils.MapperRegister;
import com.cuzz.bukkitmybatis.logging.LogSinks;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class BukkitMybatis extends JavaPlugin {

    public static BukkitMybatis instance;
    private SqlSessionFactory sqlSessionFactory ;
    private MybatisService mybatisService;
    private int testTaskId = -1;
    private final Random testRandom = new Random();
    public  SqlSessionFactory getSqlSessionFactory(){
        return this.sqlSessionFactory;
    }

    File  loadMybatiesConfig(){
        File configFile = new File(this.getDataFolder(), "mybatis-config.xml");
        //        this.saveResource("mappers/TestMapper2.xml",true);
        if (!configFile.exists()){
            this.saveResource("mybatis-config.xml", true);
        }
        return configFile;
    }

    Boolean initMybatis(){
        File configFile = loadMybatiesConfig();
        Properties xmlProperties = loadXmlProperties(configFile);
        LogSinks.init(this, xmlProperties);
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            // Build SqlSessionFactory from config and env properties.
            Properties properties = buildDbProperties(xmlProperties);
            if (properties == null) {
                return false;
            }
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(fileInputStream, properties);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private Properties loadXmlProperties(File configFile) {
        Properties properties = new Properties();
        try {
            var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            var document = builder.parse(configFile);
            NodeList nodes = document.getElementsByTagName("properties");
            if (nodes.getLength() == 0) {
                return properties;
            }
            Element propertiesNode = (Element) nodes.item(0);
            NodeList childNodes = propertiesNode.getElementsByTagName("property");
            for (int i = 0; i < childNodes.getLength(); i++) {
                Element element = (Element) childNodes.item(i);
                String name = element.getAttribute("name");
                String value = element.getAttribute("value");
                if (name != null && !name.trim().isEmpty()) {
                    properties.setProperty(name.trim(), value == null ? "" : value.trim());
                }
            }
        } catch (Exception ex) {
            this.getLogger().log(Level.WARNING, "Failed to read mybatis-config.xml properties.", ex);
        }
        return properties;
    }

    private Properties buildDbProperties(Properties xmlProperties) {
        String jdbcUrl = System.getenv("BUKKIT_MYBATIS_JDBC_URL");
        String username = System.getenv("BUKKIT_MYBATIS_USERNAME");
        String password = System.getenv("BUKKIT_MYBATIS_PASSWORD");

        if (isBlank(jdbcUrl) || isBlank(username) || isBlank(password)) {
            this.getLogger().log(Level.SEVERE,
                    "Missing required env vars: BUKKIT_MYBATIS_JDBC_URL, BUKKIT_MYBATIS_USERNAME, BUKKIT_MYBATIS_PASSWORD");
            return null;
        }

        Properties properties = new Properties();
        properties.setProperty("db.url", jdbcUrl);
        properties.setProperty("db.username", username);
        properties.setProperty("db.password", password);
        String logImpl = System.getenv("BUKKIT_MYBATIS_LOG_IMPL");
        if (isBlank(logImpl) && xmlProperties != null) {
            logImpl = xmlProperties.getProperty("mybatis.logImpl");
        }
        if (isBlank(logImpl)) {
            logImpl = "com.cuzz.bukkitmybatis.logging.BukkitMybatisLog";
        }
        properties.setProperty("mybatis.logImpl", logImpl);
        return properties;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void onEnable() {

        instance = this;

        this.getLogger().log(Level.INFO, "Downloading dependencies (async)...");
        CompletableFuture
                .supplyAsync(LibraryLoader::downLoadStart)
                .whenComplete((success, error) -> getServer().getScheduler().runTask(this, () -> {
                    if (error != null || !Boolean.TRUE.equals(success)) {
                        this.getLogger().log(Level.SEVERE, "Dependency download failed.", error);
                        getServer().getPluginManager().disablePlugin(this);
                        return;
                    }
                    finishBootstrap();
                }));
    }

    private void finishBootstrap() {
        if (!initMybatis()){
            this.getLogger().log(Level.WARNING, "Mybatis init failed.");
            // Disable plugin
            getServer().getPluginManager().disablePlugin(this);
            return;  // Exit onEnable, skip remaining work
        }else {
            this.getLogger().log(Level.INFO, "Mybatis init succeeded.");
        }

        LogSinks.log(com.cuzz.bukkitmybatis.logging.LogLevel.INFO, "BukkitMybatis", "BukkitMybatis started.", null);

        registerService();

        MapperRegister.registerMappers(this);
        startTestQueries();

        Collection<? extends Player> onlinePlayers = this.getServer().getOnlinePlayers();
        List<String> list = onlinePlayers.stream().map(Player::getName).toList();
    }

    private void registerService() {
        mybatisService = new MybatisServiceImpl(this, sqlSessionFactory);
        getServer().getServicesManager().register(MybatisService.class, mybatisService, this, ServicePriority.Normal);
    }

    public static BukkitMybatis getInstance(){
        return instance;
    }
    @Override
    public void onDisable() {
        stopTestQueries();
        closeMybatis();
        LogSinks.shutdown();
    }

    private void closeMybatis() {
        if (sqlSessionFactory == null) {
            return;
        }
        try {
            Object dataSource = sqlSessionFactory.getConfiguration()
                    .getEnvironment()
                    .getDataSource();
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        } catch (Exception ex) {
            this.getLogger().log(Level.WARNING, "Failed to close MyBatis data source.", ex);
        } finally {
            if (mybatisService != null) {
                getServer().getServicesManager().unregister(MybatisService.class, mybatisService);
                mybatisService = null;
            }
            sqlSessionFactory = null;
        }
    }

    private void startTestQueries() {
        if (testTaskId != -1) {
            return;
        }
        testTaskId = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (sqlSessionFactory == null) {
                return;
            }
            try (SqlSession session = sqlSessionFactory.openSession()) {
                TestMapper2 mapper2 = session.getMapper(TestMapper2.class);
                int choice = testRandom.nextInt(3);
                switch (choice) {
                    case 0 -> mapper2.getGroupById((long) (testRandom.nextInt(50) + 1));
                    case 1 -> {
                        String[] names = {"AAA", "BBB", "CCC", "DDD"};
                        mapper2.getGroupByName(names[testRandom.nextInt(names.length)]);
                    }
                    default -> mapper2.listGroups(testRandom.nextInt(20), testRandom.nextInt(5) + 1);
                }
            } catch (Exception ex) {
                this.getLogger().log(Level.WARNING, "Test query failed.", ex);
            }
        }, 40L, 100L).getTaskId();
    }

    private void stopTestQueries() {
        if (testTaskId == -1) {
            return;
        }
        getServer().getScheduler().cancelTask(testTaskId);
        testTaskId = -1;
    }
}
