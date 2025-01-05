# BukkitMybatis 插件文档

## 配置数据库连接

在 `mybatis-config.xml` 中配置数据库连接地址：
后续所有的依赖BukkitMybatis的插件都是在这个数据源进行CRUD操作的.
mybatis允许单个应用(进程)持有多个数据源,但是个人认为在 Minecraft 服务器这种场景中,集中管理数据库连接不仅能简化配置，
还能减少资源开销，保持服务器的高效性,所以自己开发的插件所有的表应该尽可能建在同一个库中.
并且在数据源为连接池的时候,创建多个数据源意味着要维护多组不同的连接池,这可能会导致过度的资源占用。
### 配置依赖
```xml
<dependencies>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.5.6</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.23</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>4.0.3</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>bukkitMybatis</groupId>
            <artifactId>bukkitMybatis</artifactId>
            <version>1.0</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/BukkitMybatis-1.0-SNAPSHOT.jar</systemPath>
        </dependency>
</dependencies>
### 使用MBG生成mapper
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
                <dependencies>
                    <dependency>
                        <groupId>mysql</groupId>
                        <artifactId>mysql-connector-java</artifactId>
                        <version>5.1.47</version>
                    </dependency>
                </dependencies>
            </plugin>
```

### 使用mybatis内置连接池
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

### 使用Hikari连接池
```xml
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
                <property name="jdbcUrl" value="jdbc:mysql://8.138.106.163:3306/electric_dispatch"/>
                <property name="username" value="root"/>
                <property name="password" value="root"/>
                <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
                <property name="maximumPoolSize" value="10"/>
                <property name="minimumIdle" value="5"/>
                <property name="connectionTimeout" value="30000"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
 ```
## 日志输出
若配置日志实现为LOG4J2类型,则日志将异步输出到根目录/mybatis/mybatis.log

## 自动注册映射文件

你需要在你自己的插件的enable()方法中,输入
```java
MapperRegister.registerMappers(this)
```
这可以自动将插件 mapper 资源文件夹中的所有映射文件(XML)注册到 MyBatis：
并且jar包内的mapper文件夹将会被保存到插件文件夹中(不替换),若后续你对mapper中任意xml文件的修改在重新注册后都会生效
所以当你修改了源码的mapperxml文件,若要保证其被保存到插件文件夹,请删除旧的xml文件

## 自动卸载映射文件

你需要在你自己的插件的disable()方法中,输入
```java
MapperRegister.unRegisterMappers(this);
```

使用方法，可以自动将插件 mapper 文件夹中的所有映射文件从 MyBatis 中卸载：
此方法会从 MyBatis 注册中心中卸载所有该插件的映射mapper。


## 获取SqlSessionFactory

通过 BukkitMybatis.instance.getSqlSessionFactory() 方法，你可以获取 MyBatis 的会话工厂：
```java
SqlSessionFactory sqlSessionFactory = BukkitMybatis.instance.getSqlSessionFactory();
```
该方法将返回当前 MyBatis 配置的 SqlSessionFactory 实例，你可以用它来创建数据库会话（SqlSession）进行数据库操作。

```java
SqlSessionFactory sqlSessionFactory = BukkitMybatis.instance.getSqlSessionFactory();
//默认情况 带有事务控制的
SqlSession sqlSession =sqlSessionFactory.openSession();
TestMapper2 mapper = sqlSession.getMapper(TestMapper2.class);
mapper.getGroupByName("testtest");
sqlSession.commit();
sqlSession.close();
//不带有事务控制的
SqlSession sqlSession2 =sqlSessionFactory.openSession(true);
TestMapper2 mapper2 = sqlSession2.getMapper(TestMapper2.class);
mapper2.getGroupByName("testtest");;
sqlSession.close();
```

