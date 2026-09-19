package io.github.jaycong.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import java.time.LocalDateTime;

/** 插入保留显式时间；更新刷新标记为可自动填充的 updatedAt 字段。 */
public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        var now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        var table = findTableInfo(metaObject);
        if (table != null && table.getFieldList().stream().anyMatch(field ->
                "updatedAt".equals(field.getProperty()) && field.isWithUpdateFill()
                        && LocalDateTime.class.equals(field.getPropertyType()))) {
            // strictUpdateFill 跳过非空值，无法刷新从数据库读取的实体时间。
            setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
        }
    }
}
