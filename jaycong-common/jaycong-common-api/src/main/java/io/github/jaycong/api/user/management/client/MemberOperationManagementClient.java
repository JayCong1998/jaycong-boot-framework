package io.github.jaycong.api.user.management.client;

import io.github.jaycong.api.user.management.facade.MemberOperationManagementFacade;
import io.github.jaycong.api.management.config.AdminFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

/** 仅 Admin 显式启用；写操作不自动重试。 */
@FeignClient(name = "user", contextId = "adminMemberOperationManagement", configuration = AdminFeignConfiguration.class)
public interface MemberOperationManagementClient extends MemberOperationManagementFacade {}
