package io.github.jaycong.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.mybatis.model.BaseEntity;
import io.github.jaycong.mybatis.pagination.MybatisPages;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MybatisIntegrationTest.App.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:mybatis-test;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:test-schema.sql"})
@Transactional
class MybatisIntegrationTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = MybatisIntegrationTest.class, annotationClass = Mapper.class)
    static class App { }

    @Autowired ItemMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test void paginationCountsAllRowsAndReturnsOnlyRequestedPage() {
        for (int i = 1; i <= 5; i++) {
            var item = new Item(); item.setName("item-" + i); mapper.insert(item);
        }
        var result = MybatisPages.toResult(mapper.selectPage(MybatisPages.toPage(new PageRequest(2, 2)),
                new QueryWrapper<Item>().orderByAsc("id")));
        assertThat(result.total()).isEqualTo(5);
        assertThat(result.rows()).extracting(Item::getName).containsExactly("item-3", "item-4");
        var beyond = MybatisPages.toResult(mapper.selectPage(MybatisPages.toPage(new PageRequest(4, 2)),
                new QueryWrapper<Item>().orderByAsc("id")));
        assertThat(beyond.total()).isEqualTo(5);
        assertThat(beyond.rows()).isEmpty();
    }

    @Test void insertFillsTimesAndUpdateRefreshesTimeWithoutChangingCreation() {
        var item = new Item(); item.setName("before");
        mapper.insert(item);
        assertThat(item.getId()).isPositive();
        assertThat(item.getCreatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isEqualTo(item.getCreatedAt());
        var old = LocalDateTime.of(2000, 1, 1, 0, 0);
        jdbc.update("update test_item set created_at = ?, updated_at = ? where id = ?", old, old, item.getId());
        var loaded = mapper.selectById(item.getId());
        loaded.setName("after"); loaded.setCreatedAt(LocalDateTime.of(1999, 1, 1, 0, 0));
        mapper.updateById(loaded);
        var persisted = mapper.selectById(item.getId());
        assertThat(persisted.getCreatedAt()).isEqualTo(old);
        assertThat(persisted.getUpdatedAt()).isAfter(old);
        assertThat(persisted.getName()).isEqualTo("after");
    }

    @Test void insertPreservesExplicitAuditValues() {
        var old = LocalDateTime.of(2001, 1, 1, 0, 0);
        var item = new Item(); item.setName("imported"); item.setCreatedAt(old); item.setUpdatedAt(old);
        mapper.insert(item);
        var persisted = mapper.selectById(item.getId());
        assertThat(persisted.getCreatedAt()).isEqualTo(old);
        assertThat(persisted.getUpdatedAt()).isEqualTo(old);
    }

    @Mapper
    interface ItemMapper extends BaseMapper<Item> { }

    @TableName("test_item")
    public static class Item extends BaseEntity {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
