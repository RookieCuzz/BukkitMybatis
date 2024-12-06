package com.cuzz.bukkitmybatis;

import com.cuzz.bukkitmybatis.mapper.TestMapper2;
import com.cuzz.bukkitmybatis.model.Group;
import com.cuzz.bukkitmybatis.utils.LibraryLoader;
import com.cuzz.bukkitmybatis.utils.Log4j2Config;
import com.cuzz.bukkitmybatis.utils.MapperRegister;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class BukkitMybatis extends JavaPlugin {

    public static BukkitMybatis instance;
    private SqlSessionFactory sqlSessionFactory ;
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
        //若无依赖则下载依赖
        LibraryLoader.downLoadStart();
        Log4j2Config.setUp();
        File configFile = loadMybatiesConfig();
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            // 使用 SqlSessionFactoryBuilder 通过输入流创建 SqlSessionFactory
            //使用mybatis
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(fileInputStream);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public void onEnable() {

        instance = this;

        if (!initMybatis()){
            this.getLogger().log(Level.WARNING,"Mybatis初始化失败!!!!!!");
            // 禁用插件
            getServer().getPluginManager().disablePlugin(this);
            return;  // 退出 onEnable 方法，不执行后续代码
        }else {
            this.getLogger().log(Level.INFO,"Mybatis初始化成功!!!!!!");
        }


        MapperRegister.registerMappers(this);
        // 注册完成后，你可以使用新的 Mapper
        TestMapper2 mapper2 = sqlSessionFactory.openSession().getMapper(TestMapper2.class);
        Group groupById2 = mapper2.getGroupByName("AAA");
        System.out.println("Second mapper result: " + groupById2.toString());

        Collection<? extends Player> onlinePlayers = this.getServer().getOnlinePlayers();
        List<String> list = onlinePlayers.stream().map(player -> {
            return player.getName();
        }).toList();


    }

    public static BukkitMybatis getInstance(){
        return instance;
    }
    @Override
    public void onDisable() {
        MapperRegister.unregisterMapper(this);
    }
}
