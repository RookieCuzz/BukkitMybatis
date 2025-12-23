# BukkitMybatis

BukkitMybatis is a Bukkit/Spigot plugin that provides MyBatis + HikariCP integration and auto-loads Mapper XML from your plugin.
This README is for users who install it on a server and call it from their own plugins.

## Requirements

- Java 21+
- Spigot 1.20.1+
- MySQL 8.0+ (recommended)

## Install

1. Copy `BukkitMybatis-1.0-SNAPSHOT-shaded.jar` to the server `plugins/` folder.
2. Start the server to generate the default config.

## Database config

Edit `plugins/BukkitMybatis/mybatis-config.xml` and fill in your DB connection:

```xml
<configuration>
    <settings>
        <setting name="logImpl" value="${mybatis.logImpl}"/>
    </settings>

    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="com.cuzz.bukkitmybatis.datasource.HikariDataSourceFactory">
                <property name="jdbcUrl" value="${db.url}"/>
                <property name="username" value="${db.username}"/>
                <property name="password" value="${db.password}"/>
                <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
                <property name="maximumPoolSize" value="10"/>
                <property name="minimumIdle" value="5"/>
                <property name="connectionTimeout" value="30000"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

## Environment variables

These override values in `mybatis-config.xml`.

Database (required):
- `BUKKIT_MYBATIS_JDBC_URL`
- `BUKKIT_MYBATIS_USERNAME`
- `BUKKIT_MYBATIS_PASSWORD`

Logging (optional):
- `BUKKIT_MYBATIS_LOG_IMPL` (default `com.cuzz.bukkitmybatis.logging.BukkitMybatisLog`)
- `BUKKIT_MYBATIS_LOG_LEVEL` (default `INFO`)
- `BUKKIT_MYBATIS_LOG_DIR` (default `plugins/BukkitMybatis/mybatis`)

Loki (optional):
- `BUKKIT_MYBATIS_LOKI_URL`
- `BUKKIT_MYBATIS_LOKI_TENANT`
- `BUKKIT_MYBATIS_LOKI_USERNAME`
- `BUKKIT_MYBATIS_LOKI_PASSWORD`
- `BUKKIT_MYBATIS_LOKI_LABELS` (format: `key1=value1,key2=value2`)
- `BUKKIT_MYBATIS_LOKI_DEBUG` (set to `true` to write debug logs to `plugins/BukkitMybatis/mybatis/loki-debug.log`)

Loki example (your domain):
```bash
export BUKKIT_MYBATIS_LOKI_URL="xxxxx"
export BUKKIT_MYBATIS_LOKI_TENANT=""
export BUKKIT_MYBATIS_LOKI_USERNAME=""
export BUKKIT_MYBATIS_LOKI_PASSWORD=""
export BUKKIT_MYBATIS_LOKI_LABELS="server=bukkit,env=prod"
```

Linux/macOS example:
```bash
export BUKKIT_MYBATIS_JDBC_URL="jdbc:mysql://127.0.0.1:3306/demo"
export BUKKIT_MYBATIS_USERNAME="root"
export BUKKIT_MYBATIS_PASSWORD="123456"
export BUKKIT_MYBATIS_LOG_LEVEL="INFO"
```

Windows PowerShell example:
```powershell
$env:BUKKIT_MYBATIS_JDBC_URL="jdbc:mysql://127.0.0.1:3306/demo"
$env:BUKKIT_MYBATIS_USERNAME="root"
$env:BUKKIT_MYBATIS_PASSWORD="123456"
$env:BUKKIT_MYBATIS_LOG_LEVEL="INFO"
```

Windows CMD (bat) example:
```bat
@echo off
set BUKKIT_MYBATIS_JDBC_URL=jdbc:mysql://127.0.0.1:3306/demo
set BUKKIT_MYBATIS_USERNAME=root
set BUKKIT_MYBATIS_PASSWORD=123456
set BUKKIT_MYBATIS_LOG_LEVEL=INFO
```

## Use in your plugin

### 1) Declare dependency

Add to your plugin `plugin.yml`:

```yaml
depend: [BukkitMybatis]
```

Maven (SNAPSHOT example):
```xml
<repositories>
  <repository>
    <id>nexus-snapshots</id>
    <url>https://www.4399mc.cn/nexus/repository/maven-snapshots/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.cuzz</groupId>
    <artifactId>BukkitMybatis</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

Gradle (SNAPSHOT example):
```gradle
repositories {
    maven { url "https://www.4399mc.cn/nexus/repository/maven-snapshots/" }
}

dependencies {
    compileOnly "com.cuzz:BukkitMybatis:1.0-SNAPSHOT"
}
```

### 2) Place Mapper XML

Put your Mapper XML under `src/main/resources/mappers/`, e.g.:

```
src/main/resources/mappers/YourMapper.xml
```

At runtime BukkitMybatis extracts and loads these XML files from your plugin JAR.

### 3) Register mappers

```java
public class YourPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        MapperRegister.registerMappers(this);
    }

    @Override
    public void onDisable() {
        MapperRegister.unregisterMapper(this);
    }
}
```

### 4) Get SqlSessionFactory and call

```java
try (SqlSession session = BukkitMybatis.getInstance().getSqlSessionFactory().openSession(true)) {
    YourMapper mapper = session.getMapper(YourMapper.class);
    // TODO: your DB calls
}
```

### 5) Run DB calls on virtual threads

```java
VirtualDbExecutor.run(session -> {
    TestMapper2 mapper = session.getMapper(TestMapper2.class);
    Group group = mapper.getGroupByName("AAA");
    getLogger().info("group=" + group);
});
```

Optional on plugin disable:

```java
VirtualDbExecutor.shutdown();
```

## Example (test table + mapper)

### Schema (MySQL)

```sql
CREATE TABLE ed_group (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  leader VARCHAR(64) DEFAULT NULL,
  leader_id BIGINT DEFAULT NULL,
  sort INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO ed_group (name, leader, leader_id, sort, created_at, updated_at) VALUES
('Alpha', 'Steve', 1001, 1, NOW(), NOW()),
('Bravo', 'Alex', 1002, 2, NOW(), NOW()),
('Charlie', 'Herobrine', 1003, 3, NOW(), NOW()),
('Delta', 'Notch', 1004, 4, NOW(), NOW()),
('Echo', 'Cuzz', 1005, 5, NOW(), NOW()),
('Foxtrot', 'Builder', 1006, 6, NOW(), NOW());
```

### Mapper interface and XML

- Interface: `src/main/java/com/cuzz/bukkitmybatis/mapper/TestMapper2.java`
- XML: `src/main/resources/mappers/TestMapper1.xml`

### Usage example

```java
try (SqlSession session = BukkitMybatis.getInstance().getSqlSessionFactory().openSession(true)) {
    TestMapper2 mapper = session.getMapper(TestMapper2.class);
    Group group = mapper.getGroupByName("AAA");
    getLogger().info("group=" + group);
}
```

## Build & publish (maintainers)

```bash
mvn -DskipTests package
mvn -DskipTests deploy
```

SNAPSHOT versions are published to `distributionManagement` `nexus-snapshots`.
