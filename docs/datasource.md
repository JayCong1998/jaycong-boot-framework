# Datasource 模块使用说明

## 接入

导入框架 BOM 后添加模块。该模块传递引入 MySQL JDBC 驱动（runtime）；使用其他数据库的项目可在业务 POM 中排除 `mysql-connector-j` 并添加对应驱动：

```xml
<dependency>
    <groupId>io.github.jaycong</groupId>
    <artifactId>jaycong-common-datasource</artifactId>
</dependency>
```

模块自动注册 MyBatis-Plus 分页、乐观锁插件、逻辑删除默认值和审计字段填充。Mapper 使用 `@Mapper`
并放在应用扫描包下，或由业务配置 `@MapperScan`。无需扫描 `io.github.jaycong.datasource`。
保留 MyBatis-Plus 原生 `mybatis-plus.*` 配置。

## 基础实体和审计字段

服务的数据库实体统一继承 `io.github.jaycong.core.model.BaseEntity`（位于 common-core）：

```java
import io.github.jaycong.core.model.BaseEntity;

@TableName("example_product")
public class Product extends BaseEntity {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@Mapper
public interface ProductMapper extends BaseMapper<Product> { }
```

数据库表采用 `id BIGINT` 应用生成主键，以及 `create_time`、`update_time`、`create_user_id`、`update_user_id`、`deleted` 和 `lock_version` 字段。MySQL 时间列使用 `DATETIME(3)`。旧包 `io.github.jaycong.datasource.model.BaseEntity` 仅作为已弃用的兼容入口保留。

- `createTime`、`updateTime` 使用 `LocalDateTime`；创建人和更新人默认填充为系统用户 `0`。
- 插入时填充空审计值；显式设置的值予以保留，方便导入历史记录。`deleted` 默认填充 `0`，`lockVersion` 默认填充 `0`。
- 携带实体更新时刷新 `updateTime`；普通实体更新不写入创建字段。
- 自动填充依赖 MyBatis-Plus 的实体与填充注解。`update(null, wrapper)`、仅 Wrapper 更新、
  自定义 SQL 等路径不保证填充，调用方应显式维护时间；Wrapper 或自定义 SQL 也可以绕过创建时间的更新策略。
- `deleted` 默认采用 `0` 未删除、`1` 已删除。自定义实体使用 `@TableLogic` 标记逻辑删除字段，使用 `@Version` 标记乐观锁字段；版本检查由默认拦截器执行。

审计填充器遵循字段上的 `FieldFill` 标记；自定义实体的审计字段需配置对应注解。
接口写入建议使用专用 DTO，避免客户端控制主键和审计字段。

## 分页

```java
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.datasource.pagination.MybatisPages;

var page = mapper.selectPage(
    MybatisPages.<Product>toPage(new PageRequest(2, 20)),
    new LambdaQueryWrapper<Product>().orderByAsc(Product::getId));
PageResult<Product> result = MybatisPages.toResult(page);
```

`page` 从 1 开始，`size` 范围 1–1000，默认 20；Core 对无效参数抛出异常。
HTTP 接口应在边界验证参数，可使用 Bean Validation 对无效参数返回 400。
转换保留总记录数，返回 `{ "total": 42, "rows": [...] }`，行列表是不可修改的快照。
查询必须启用 count，不能将未知总数的分页结果传入该转换器。
超出末页返回空列表，不自动跳回第一页。默认插件根据连接识别数据库方言，单页限制 1000。
分页排序应由服务端确定，不能直接拼接客户端字段。

## 覆盖默认行为

业务声明 `MybatisPlusInterceptor` Bean 后，模块不再创建默认拦截器；业务负责配置全部插件，包括乐观锁和分页插件。组合多个插件时，将分页插件放在最后。
声明任意 `MetaObjectHandler` Bean 后，默认审计填充器退让；业务负责需要保留的填充行为。

例如固定 MySQL 方言：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    var pagination = new PaginationInnerInterceptor(DbType.MYSQL);
    pagination.setMaxLimit(1000L);
    var interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    interceptor.addInnerInterceptor(pagination);
    return interceptor;
}
```

参考 MyBatis-Plus 官方[分页插件说明](https://baomidou.com/plugins/pagination/)
和[自动填充说明](https://baomidou.com/guides/auto-fill-field/)。
