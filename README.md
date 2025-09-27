# BukkitMybatis

[![Java](https://img.shields.io/badge/Java-16+-orange.svg)](https://www.oracle.com/java/)
[![Spigot](https://img.shields.io/badge/Spigot-1.20.1-yellow.svg)](https://www.spigotmc.org/)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.5.6-blue.svg)](https://mybatis.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

BukkitMybatis 是一个为 Minecraft Bukkit/Spigot 插件开发者设计的 MyBatis 集成库。它提供了简单易用的数据库操作接口，支持连接池管理、自动映射器注册、日志配置等功能，让插件开发者能够轻松地在 Minecraft 服务器中使用 MyBatis 进行数据库操作。

## ✨ 特性

- 🚀 **即插即用**: 自动下载和管理依赖，无需手动配置复杂的类路径
- 🔄 **自动映射器管理**: 支持映射器的自动注册和卸载
- 🏊 **连接池支持**: 内置 HikariCP 连接池支持，提供高性能数据库连接
- 📝 **灵活的日志配置**: 支持 Log4j2 异步日志输出
- 🔧 **热重载支持**: 支持映射器文件的热重载和更新
- 🛡️ **线程安全**: 提供线程安全的数据库操作环境
- 📦 **统一数据源管理**: 所有依赖插件共享同一个数据源，减少资源开销

## 📋 系统要求

- Java 16+
- Bukkit/Spigot 1.20.1+
- MySQL 8.0+ (推荐)

## 🚀 快速开始

### 1. 安装 BukkitMybatis

将 `BukkitMybatis-1.0-SNAPSHOT.jar` 放入服务器的 `plugins` 目录中，启动服务器。插件会自动：
- 下载所需的依赖库
- 创建默认配置文件
- 初始化 MyBatis 环境

### 2. 配置数据库连接

启动后，在 `plugins/BukkitMybatis/mybatis-config.xml` 中配置数据库连接：

#### 使用 HikariCP 连接池（推荐）

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 配置日志工厂类型 -->
    <settings>
        <setting name="logImpl" value="LOG4J2"/> <!-- [LOG4J2,STDOUT_LOGGING] -->
    </settings>

    <!-- 配置数据库连接池 -->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="com.cuzz.bukkitmybatis.datasource.HikariDataSourceFactory">
                <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/your_database"/>
                <property name="username" value="your_username"/>
                <property name="password" value="your_password"/>
                <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
                <property name="maximumPoolSize" value="10"/>
                <property name="minimumIdle" value="5"/>
                <property name="connectionTimeout" value="30000"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

#### 使用 MyBatis 内置连接池

```xml
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/your_database?useSSL=false"/>
                <property name="username" value="your_username"/>
                <property name="password" value="your_password"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

## 🔧 在你的插件中使用

### 1. 添加依赖

在你的插件项目的 `pom.xml` 中添加以下依赖：

```xml
<dependencies>
    <!-- MyBatis 核心 -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.6</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.23</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- HikariCP 连接池 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>4.0.3</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- BukkitMybatis -->
    <dependency>
        <groupId>bukkitMybatis</groupId>
        <artifactId>bukkitMybatis</artifactId>
        <version>1.0</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/libs/BukkitMybatis-1.0-SNAPSHOT.jar</systemPath>
    </dependency>
</dependencies>
```

### 2. 在 plugin.yml 中声明依赖

```yaml
name: YourPlugin
version: 1.0.0
main: com.yourpackage.YourPlugin
api-version: '1.20'
depend: [BukkitMybatis]  # 声明依赖 BukkitMybatis
```

### 3. 创建实体类

```java
package com.yourpackage.model;

public class User {
    private Long id;
    private String username;
    private String email;
    private Date createTime;
    
    // 构造函数、getter 和 setter 方法
    public User() {}
    
    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.createTime = new Date();
    }
    
    // getter 和 setter 方法...
}
```

### 4. 创建 Mapper 接口

```java
package com.yourpackage.mapper;

import com.yourpackage.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User getUserById(@Param("id") Long id);
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    User getUserByUsername(@Param("username") String username);
    
    @Insert("INSERT INTO users(username, email, create_time) VALUES(#{username}, #{email}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);
    
    @Update("UPDATE users SET email = #{email} WHERE id = #{id}")
    int updateUser(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteUser(@Param("id") Long id);
    
    @Select("SELECT * FROM users")
    List<User> getAllUsers();
}
```

### 5. 创建 XML 映射文件（可选）

在 `src/main/resources/mapper/` 目录下创建 `UserMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.yourpackage.mapper.UserMapper">
    
    <resultMap id="UserResultMap" type="com.yourpackage.model.User">
        <id property="id" column="id"/>
        <result property="username" column="username"/>
        <result property="email" column="email"/>
        <result property="createTime" column="create_time"/>
    </resultMap>
    
    <select id="getUserById" resultMap="UserResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>
    
    <select id="getUsersByEmail" resultMap="UserResultMap">
        SELECT * FROM users WHERE email LIKE CONCAT('%', #{email}, '%')
    </select>
    
    <insert id="batchInsertUsers" parameterType="java.util.List">
        INSERT INTO users(username, email, create_time) VALUES
        <foreach collection="list" item="user" separator=",">
            (#{user.username}, #{user.email}, #{user.createTime})
        </foreach>
    </insert>
    
</mapper>
```

### 6. 在插件主类中注册和使用

```java
package com.yourpackage;

import com.cuzz.bukkitmybatis.BukkitMybatis;
import com.cuzz.bukkitmybatis.utils.MapperRegister;
import com.yourpackage.mapper.UserMapper;
import com.yourpackage.model.User;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
    
    private SqlSessionFactory sqlSessionFactory;
    
    @Override
    public void onEnable() {
        // 获取 SqlSessionFactory
        sqlSessionFactory = BukkitMybatis.getInstance().getSqlSessionFactory();
        
        // 注册映射器
        MapperRegister.registerMappers(this);
        
        // 测试数据库连接
        testDatabaseConnection();
        
        getLogger().info("插件启用成功！");
    }
    
    @Override
    public void onDisable() {
        // 卸载映射器
        MapperRegister.unRegisterMappers(this);
        getLogger().info("插件已禁用！");
    }
    
    private void testDatabaseConnection() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 创建新用户
            User newUser = new User("testuser", "test@example.com");
            mapper.insertUser(newUser);
            getLogger().info("创建用户成功，ID: " + newUser.getId());
            
            // 查询用户
            User user = mapper.getUserById(newUser.getId());
            getLogger().info("查询用户: " + user.getUsername());
            
        } catch (Exception e) {
            getLogger().severe("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 提供给其他类使用的方法
    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
}
```

## 📚 高级用法

### 事务管理

```java
// 手动事务控制
SqlSession session = sqlSessionFactory.openSession(); // 默认不自动提交
try {
    UserMapper mapper = session.getMapper(UserMapper.class);
    
    // 执行多个操作
    User user1 = new User("user1", "user1@example.com");
    User user2 = new User("user2", "user2@example.com");
    
    mapper.insertUser(user1);
    mapper.insertUser(user2);
    
    // 手动提交事务
    session.commit();
    getLogger().info("事务提交成功");
    
} catch (Exception e) {
    // 回滚事务
    session.rollback();
    getLogger().severe("事务回滚: " + e.getMessage());
} finally {
    session.close();
}

// 自动提交模式
try (SqlSession session = sqlSessionFactory.openSession(true)) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    mapper.insertUser(new User("autocommit", "auto@example.com"));
    // 自动提交，无需手动调用 commit()
}
```

### 批量操作

```java
try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    
    for (int i = 0; i < 1000; i++) {
        User user = new User("user" + i, "user" + i + "@example.com");
        mapper.insertUser(user);
    }
    
    session.commit();
    getLogger().info("批量插入完成");
}
```

### 异步数据库操作

```java
import org.bukkit.scheduler.BukkitRunnable;

public void asyncDatabaseOperation() {
    new BukkitRunnable() {
        @Override
        public void run() {
            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                UserMapper mapper = session.getMapper(UserMapper.class);
                List<User> users = mapper.getAllUsers();
                
                // 回到主线程更新 UI 或发送消息
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 主线程操作
                        getLogger().info("查询到 " + users.size() + " 个用户");
                    }
                }.runTask(YourPlugin.this);
                
            } catch (Exception e) {
                getLogger().severe("异步数据库操作失败: " + e.getMessage());
            }
        }
    }.runTaskAsynchronously(this);
}
```

## 🔧 配置选项

### HikariCP 连接池配置

| 参数 | 说明 | 默认值 | 推荐值 |
|------|------|--------|--------|
| `maximumPoolSize` | 最大连接数 | 10 | 10-20 |
| `minimumIdle` | 最小空闲连接数 | 5 | 5-10 |
| `connectionTimeout` | 连接超时时间(ms) | 30000 | 30000 |
| `idleTimeout` | 空闲超时时间(ms) | 600000 | 600000 |
| `maxLifetime` | 连接最大生存时间(ms) | 1800000 | 1800000 |

### 日志配置

支持的日志实现：
- `LOG4J2`: 异步日志输出到 `mybatis/mybatis.log`
- `STDOUT_LOGGING`: 控制台输出
- `NO_LOGGING`: 禁用日志

## 🛠️ 开发工具

### 使用 MyBatis Generator

在 `pom.xml` 中添加 MyBatis Generator 插件：

```xml
<plugin>
    <groupId>org.mybatis.generator</groupId>
    <artifactId>mybatis-generator-maven-plugin</artifactId>
    <version>1.3.2</version>
    <configuration>
        <configurationFile>${basedir}/src/main/resources/mybatis-generator.xml</configurationFile>
        <overwrite>true</overwrite>
        <verbose>true</verbose>
    </configuration>
</plugin>
```

创建 `mybatis-generator.xml` 配置文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">

<generatorConfiguration>
    <context id="mysql" targetRuntime="MyBatis3">
        <jdbcConnection driverClass="com.mysql.cj.jdbc.Driver"
                        connectionURL="jdbc:mysql://localhost:3306/your_database"
                        userId="your_username"
                        password="your_password"/>

        <javaModelGenerator targetPackage="com.yourpackage.model"
                            targetProject="src/main/java"/>

        <sqlMapGenerator targetPackage="mapper"
                         targetProject="src/main/resources"/>

        <javaClientGenerator type="XMLMAPPER"
                             targetPackage="com.yourpackage.mapper"
                             targetProject="src/main/java"/>

        <table tableName="users" domainObjectName="User"/>
    </context>
</generatorConfiguration>
```

运行生成器：
```bash
mvn mybatis-generator:generate
```

## 🐛 故障排除

### 常见问题

1. **插件启动失败**
   - 检查 Java 版本是否为 16+
   - 确认数据库连接配置正确
   - 查看控制台错误日志

2. **映射器注册失败**
   - 确认 XML 文件语法正确
   - 检查 namespace 是否与接口全限定名一致
   - 验证 SQL 语句语法

3. **数据库连接失败**
   - 检查数据库服务是否启动
   - 验证连接字符串、用户名、密码
   - 确认防火墙设置

4. **依赖冲突**
   - 确保所有依赖的 scope 设置为 `provided`
   - 检查是否有其他插件使用了不同版本的相同依赖

### 调试技巧

1. **启用详细日志**
   ```xml
   <setting name="logImpl" value="STDOUT_LOGGING"/>
   ```

2. **检查映射器注册状态**
   ```java
   Configuration config = sqlSessionFactory.getConfiguration();
   Collection<Class<?>> mappers = config.getMapperRegistry().getMappers();
   for (Class<?> mapper : mappers) {
       getLogger().info("已注册映射器: " + mapper.getName());
   }
   ```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [MyBatis](https://mybatis.org/) - 优秀的持久层框架
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - 高性能连接池
- [Spigot](https://www.spigotmc.org/) - Minecraft 服务器平台

---

如果这个项目对你有帮助，请给个 ⭐ Star！

