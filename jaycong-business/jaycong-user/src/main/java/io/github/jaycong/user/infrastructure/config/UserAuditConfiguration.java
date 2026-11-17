package io.github.jaycong.user.infrastructure.config;

import io.github.jaycong.satoken.context.AuthContext;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.jaycong.datasource.handler.AuditMetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 内部管理请求透传已验证的会话，会员资料审计记录实际操作人。 */
@Configuration(proxyBeanMethods = false)
public class UserAuditConfiguration {
    @Bean
    public MetaObjectHandler userAuditHandler(AuthContext authContext) {
        return new AuditMetaObjectHandler() {
            private long actor() { return authContext.isLogin() ? authContext.requireUserId() : 0L; }
            @Override public void insertFill(MetaObject metaObject) {
                if (getFieldValByName("createUserId", metaObject) == null)
                    setFieldValByName("createUserId", actor(), metaObject);
                if (getFieldValByName("updateUserId", metaObject) == null)
                    setFieldValByName("updateUserId", actor(), metaObject);
                super.insertFill(metaObject);
            }
            @Override public void updateFill(MetaObject metaObject) {
                super.updateFill(metaObject);
                setFieldValByName("updateUserId", actor(), metaObject);
            }
        };
    }
}
