# MyBatis Starter 使用说明

## 接入

导入框架 BOM 后添加 Starter，数据库驱动由业务项目自行引入：

```xml
<dependency>
    <groupId>io.github.jaycong</groupId>
    <artifactId>jaycong-mybatis-spring-boot-starter</artifactId>
</dependency>
```

Starter 自动注册 MyBatis-Plus 分页插件和时间审计填充器。Mapper 使用 `@Mapper`
并放在应用扫描包下，或由业务配置 `@MapperScan`。无需扫描 `io.github.jaycong.mybatis`。
保留 MyBatis-Plus 原生 `mybatis-plus.*` 配置。

## 基础实体和时间字段

业务实体可继承 `io.github.jaycong.mybatis.model.BaseEntity`：

```java
@TableName("example_product")
public class Product extends BaseEntity {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@Mapper
public interface ProductMapper extends BaseMapper<Product> { }
```

数据库表需要 `id`（BIGINT 自增主键）、`created_at`、`updated_at`。
MySQL 时间列建议使用 `DATETIME(6)`。基础实体是可选约定，业务也可独立定义自己的主键和实体。

- `createdAt`、`updatedAt` 使用 `LocalDateTime`，时间来自应用服务器本地时区。
- 插入时填充空时间；显式设置的时间予以保留，方便导入历史记录。
- 携带实体更新时刷新 `updatedAt`，即使实体中原来已有值；普通实体更新不写入 `createdAt`。
- 自动填充依赖 MyBatis-Plus 的实体与填充注解。`update(null, wrapper)`、仅 Wrapper 更新、
  自定义 SQL 等路径不保证填充，调用方应显式维护时间；Wrapper 或自定义 SQL 也可以绕过创建时间的更新策略。
- 本版不包含用户审计、逻辑删除、乐观锁或租户隔离。

审计填充器遵循字段上的 `FieldFill` 标记；自定义实体的时间字段需配置对应注解。
接口写入建议使用专用 DTO，避免客户端控制主键和审计字段，示例已采用此方式。

## 分页

```java
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.mybatis.pagination.MybatisPages;

var page = mapper.selectPage(
    MybatisPages.<Product>toPage(new PageRequest(2, 20)),
    new LambdaQueryWrapper<Product>().orderByAsc(Product::getId));
PageResult<Product> result = MybatisPages.toResult(page);
```

`page` 从 1 开始，`size` 范围 1–1000，默认 20；Core 对无效参数抛出异常。
HTTP 接口应在边界验证参数，Example 使用 Bean Validation 返回 400。
转换保留总记录数，返回 `{ "total": 42, "rows": [...] }`，行列表是不可修改的快照。
查询必须启用 count，不能将未知总数的分页结果传入该转换器。
超出末页返回空列表，不自动跳回第一页。默认插件根据连接识别数据库方言，单页限制 1000。
分页排序应由服务端确定，不能直接拼接客户端字段。

## 覆盖默认行为

业务声明 `MybatisPlusInterceptor` Bean 后，Starter 不再创建默认拦截器；业务负责配置
全部插件，包括分页插件。组合多个插件时，将分页插件放在最后。
声明任意 `MetaObjectHandler` Bean 后，默认时间填充器退让；业务负责需要保留的填充行为。

例如固定 MySQL 方言：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    var pagination = new PaginationInnerInterceptor(DbType.MYSQL);
    pagination.setMaxLimit(1000L);
    var interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(pagination);
    return interceptor;
}
```

参考 MyBatis-Plus 官方[分页插件说明](https://baomidou.com/plugins/pagination/)
和[自动填充说明](https://baomidou.com/guides/auto-fill-field/)。

## 运行 Example

```powershell
mvn -B -ntp clean verify
java -jar jaycong-example/target/jaycong-example-0.1.0-SNAPSHOT.jar
```

默认使用独立的 H2 内存数据库，自动建表，重启后数据清空。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/example/products` | 新增，JSON `{"name":"Book"}` |
| GET | `/example/products/{id}` | 按 ID 查询 |
| GET | `/example/products?page=1&size=20` | 按 ID 升序分页 |
| PUT | `/example/products/{id}` | 修改名称，JSON `{"name":"Notebook"}` |
| DELETE | `/example/products/{id}` | 物理删除 |

响应使用 Core `Result`；成功 HTTP 200，不存在记录返回 HTTP 404，空白/超长名称或无效分页返回 HTTP 400。
示例不包含鉴权，面向本地学习和集成验证。

```powershell
Invoke-RestMethod http://localhost:8080/example/products -Method Post -ContentType 'application/json' -Body '{"name":"Book"}'
Invoke-RestMethod 'http://localhost:8080/example/products?page=1&size=20'
```

## 使用 MySQL

1. 建立 `jaycong_example` 数据库，在目标库手动执行
   `jaycong-example/src/main/resources/db/schema-mysql.sql`。
2. 设置连接信息并启用 profile：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/jaycong_example'
$env:DB_USERNAME = 'your_username'
$env:DB_PASSWORD = 'your_password'
java -jar jaycong-example/target/jaycong-example-0.1.0-SNAPSHOT.jar --spring.profiles.active=mysql
```

MySQL profile 禁止自动初始化 SQL，不自动创建或清理外部库。本次自动化测试使用真实 H2，
不等同于 MySQL 服务端兼容性验证；连接实际 MySQL 后应再执行所需业务查询验证。

## 验证

`MybatisIntegrationTest` 验证真实 Mapper 分页、总数、超页、插入和更新审计字段；
`MybatisAutoConfigurationTest` 验证默认配置及用户 Bean 覆盖；
`ProductHttpTest` 验证真实 HTTP CRUD、分页与错误响应。全量验证同时运行原有 Core 和 Web 测试。
