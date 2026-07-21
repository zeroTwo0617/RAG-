# Java 后端学习笔记

## Spring Boot 自动配置

Spring Boot 的自动配置基于 `@Conditional` 系列注解和条件化 Bean。启动时会扫描 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中声明的配置类，根据 classpath 是否存在某依赖、是否配置了某属性来决定是否生效。这让我们"开箱即用"地获得 Tomcat、数据源等默认装配。

## MyBatis 与 SQL 掌控

MyBatis 让我们把 SQL 握在自己手里。使用 MyBatis-Plus 后，基础 CRUD 由 `BaseMapper` 提供，复杂查询仍写 XML 或注解 SQL。`@Param` 用于多参数映射，`Result` 注解做列名到字段的映射，`TypeHandler` 可自定义 JDBC 类型转换（例如把 `float[]` 与 PostgreSQL 的 `vector` 类型互转）。

## 事务管理

`@Transactional` 声明式事务基于 AOP 代理。默认只在抛出运行时异常时回滚。传播行为（Propagation）决定方法间调用时事务如何复用或挂起。注意：同类方法内部调用 `@Transactional` 不生效，因为绕过了代理。

## JWT 鉴权

JWT 由 header、payload、signature 组成，服务端用密钥签名，客户端携带于 `Authorization` 头。无状态、易横向扩展。风险点是密钥泄露与无法主动注销，通常用短过期时间 + 刷新令牌缓解。
