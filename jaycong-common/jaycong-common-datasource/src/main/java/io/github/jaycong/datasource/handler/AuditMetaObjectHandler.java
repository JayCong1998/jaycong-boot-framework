package io.github.jaycong.datasource.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import java.time.LocalDateTime;

/** 默认填充审计时间和系统操作人；实体可通过字段 fill 策略选择需要自动维护的字段。 */
public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        var now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createUserId", Long.class, 0L);
        strictInsertFill(metaObject, "updateUserId", Long.class, 0L);
        strictInsertFill(metaObject, "deleted", Integer.class, 0);
        strictInsertFill(metaObject, "lockVersion", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        var table = findTableInfo(metaObject);
        if (table != null && table.getFieldList().stream().anyMatch(field ->
                "updateTime".equals(field.getProperty()) && field.isWithUpdateFill()
                        && LocalDateTime.class.equals(field.getPropertyType()))) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
        var auditUser = findTableInfo(metaObject);
        if (auditUser != null && auditUser.getFieldList().stream().anyMatch(field ->
                "updateUserId".equals(field.getProperty()) && field.isWithUpdateFill()
                        && Long.class.equals(field.getPropertyType()))) {
            setFieldValByName("updateUserId", 0L, metaObject);
        }
    }
}
