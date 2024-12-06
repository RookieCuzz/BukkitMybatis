# BukkitMybatis 插件文档

## 配置数据库连接

在 `mybatis-config.xml` 中配置数据库连接地址：

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
</configuration>```
# 自动注册映射文件

使用 MapperRegister.registerMappers(JavaPlugin plugin) 方法，可以自动将插件 mapper 文件夹中的所有映射文件注册到 MyBatis：

MapperRegister.registerMappers(plugin);

此方法会扫描插件的 mapper 文件夹，并将所有映射文件加载到 MyBatis 中。

自动卸载映射文件

使用 MapperRegister.unRegisterMappers(JavaPlugin plugin) 方法，可以自动将插件 mapper 文件夹中的所有映射文件从 MyBatis 中卸载：

MapperRegister.unRegisterMappers(plugin);

此方法会从 MyBatis 配置中卸载所有插件的映射文件。
获取 SqlSessionFactory

通过 BukkitMybatis.instance.getSqlSessionFactory() 方法，你可以获取 MyBatis 的会话工厂：

SqlSessionFactory sqlSessionFactory = BukkitMybatis.instance.getSqlSessionFactory();

该方法将返回当前 MyBatis 配置的 SqlSessionFactory 实例，你可以用它来创建数据库会话（SqlSession）进行数据库操作。
