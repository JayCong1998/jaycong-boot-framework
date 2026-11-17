package io.github.jaycong.api.goods.management.client;

import io.github.jaycong.api.goods.management.facade.GoodsManagementFacade;
import io.github.jaycong.api.management.config.AdminFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

/** 仅 Admin 显式启用；写操作不自动重试。 */
@FeignClient(name = "goods", contextId = "adminGoodsManagement", configuration = AdminFeignConfiguration.class)
public interface GoodsManagementClient extends GoodsManagementFacade {}
