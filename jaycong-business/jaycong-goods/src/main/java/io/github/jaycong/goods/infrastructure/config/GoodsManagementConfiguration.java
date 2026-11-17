package io.github.jaycong.goods.infrastructure.config;

import io.github.jaycong.satoken.context.AuthContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.jaycong.api.user.client.UserFeignClient;
import io.github.jaycong.datasource.handler.AuditMetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = UserFeignClient.class)
public class GoodsManagementConfiguration {
    /** 拒绝小数到整数的静默截断，库存数量、状态、版本及ID必须按原值校验。 */
    @Bean public Jackson2ObjectMapperBuilderCustomizer goodsStrictIntegerBinding() {
        return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
    /** 覆盖公共默认填充器，为后台写入保留实际操作人；无会话仍用系统用户0。 */
    @Bean public MetaObjectHandler goodsAuditHandler(AuthContext authContext) {
        return new AuditMetaObjectHandler() {
            private long actor() { return authContext.isLogin() ? authContext.requireUserId() : 0L; }
            @Override public void insertFill(MetaObject metaObject) {
                super.insertFill(metaObject);
                setFieldValByName("createUserId", actor(), metaObject);
                setFieldValByName("updateUserId", actor(), metaObject);
            }
            @Override public void updateFill(MetaObject metaObject) {
                super.updateFill(metaObject); setFieldValByName("updateUserId", actor(), metaObject);
            }
        };
    }
}
