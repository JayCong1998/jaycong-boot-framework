# 完整案例：设备借用申请审批

本章是独立的教学用例，说明从接口到实体、事务、SQL、操作记录和结果处理的完整职责链。业务名、字段和错误码只属于这个示例，生成目标代码时替换为目标业务语言。代码片段省略 import、构造器、访问器及工程公共组件，不是已经编译运行的独立工程。

## 1. 业务范围与规则

员工提交设备借用申请，负责人批准或驳回。本案例只负责申请审批，不负责实际锁定设备、出库或供应商操作；实际资源分配若进入用例，需要另行定义跨业务流程。

| 状态 | 允许的动作 | 目标状态 |
|---|---|---|
| DRAFT | submit / cancel | SUBMITTED / CANCELLED |
| SUBMITTED | approve / reject / cancel | APPROVED / REJECTED / CANCELLED |
| APPROVED / REJECTED / CANCELLED | 无后续状态动作 | 保持终态 |

批准用例的具体规则：

- 操作者来自可信身份上下文，必须具备该租户及申请范围内的审批权限。
- 只能批准 SUBMITTED 状态。不同操作号重复批准已批准申请，返回 ALREADY_APPROVED；同操作号成功重试返回原结果。
- 幂等范围为 tenantId + applicationId + APPROVE + identifier；保存操作者和关键载荷摘要，同键不同操作者或不同载荷拒绝为 IDEMPOTENCY_CONFLICT。
- 审批状态更新和成功操作记录在同一数据库事务内提交。
- 审批与取消并发时，通过版本条件只允许一方成功；另一方返回 CONCURRENT_MODIFICATION，可重新读取后决定是否重试。

这些规则是本案例的明确假设；多级审批、代理审批、审批后撤回等不在假设中。

## 2. 包与类

根包以 `com.example` 表示，不能原样套用到目标工程。

```text
com.example.api.loan/
  LoanFacadeService.java
  request/ApproveLoanRequest.java
  response/LoanOperationResponse.java
  model/LoanApplicationVO.java

com.example.loan/
  controller/LoanController.java
  param/ApproveLoanParam.java
  facade/LoanFacadeServiceImpl.java
  domain/entity/LoanApplication.java
  domain/entity/LoanOperation.java
  domain/entity/convertor/LoanConvertor.java
  domain/service/LoanManageService.java
  domain/service/LoanReadService.java     # 列表/详情复杂时选用
  infrastructure/mapper/LoanApplicationMapper.java
  infrastructure/mapper/LoanOperationMapper.java
```

该用例是本地审批，不需要 ApplicationService、外部渠道、消息或工厂；状态简单，直接在实体校验，不单独增加状态机。实体直接参与持久化，领域服务直接使用 Mapper。公开契约可放在独立模块或 API 包。

## 3. 输入与返回

HTTP 路径指定 applicationId，Param 仅包含 identifier 与审批备注。Controller 从可信会话/网关上下文取得 tenantId、actorId，再构造内部调用的公开 Request。若公开 Request 经 RPC 暴露，RPC 边界也必须确认这些身份来源可信。

```java
// HTTP 输入，不允许客户端选择审批人或租户。
public record ApproveLoanParam(String identifier, String comment) {}

// 公开契约；tenantId / actorId 由可信入口提供。
public record ApproveLoanRequest(
    String tenantId, String applicationId, String actorId,
    String identifier, String comment) {}

public record LoanOperationResponse(
    boolean success, String code, String applicationId,
    String operationId, String resultState) {}
```

入口验证 ID/幂等标识非空、备注长度等格式。重复成功使用 code=DUPLICATED、success=true；resultState 是那次操作保存的结果，不表示重新执行或反映对象之后的最新状态。最新状态通过查询接口取得。

## 4. 实体只维护自身规则

```java
public void approve(String approverId, String comment, Instant time) {
    if (state != LoanState.SUBMITTED) {
        throw new LoanException("INVALID_STATE");
    }
    this.state = LoanState.APPROVED;
    this.approverId = approverId;
    this.approvalComment = comment;
    this.approvedAt = time;
}
```

先检查前置状态，再一起修改相关字段。权限需要外部身份与范围信息，由领域服务完成；实体不注入 Mapper、身份服务、HTTP 客户端或消息组件。

## 5. 领域服务控制本地写入

以下为职责示意，`authorization` 表示审批权限能力，`requestFingerprint` 对明确的业务字段做稳定规范化后计算摘要，`clock` 提供时间；这些名称描述需要实现的协作角色，不是某个工程已存在的组件。

```java
@Transactional(rollbackFor = Exception.class)
public LoanOperationResponse approve(ApproveLoanRequest request) {
    validateRequiredInput(request);
    LoanApplication loan = applicationMapper.selectByTenantAndId(
        request.tenantId(), request.applicationId());
    if (loan == null) throw new LoanException("NOT_FOUND");

    authorization.checkCanApprove(request.actorId(), loan);
    String fingerprint = requestFingerprint.of(request.comment());
    LoanOperation previous = operationMapper.selectByKey(
        request.tenantId(), request.applicationId(), "APPROVE",
        request.identifier());
    if (previous != null) {
        previous.assertSameRequest(request.actorId(), fingerprint);
        return previous.toResponse("DUPLICATED");
    }

    if (loan.getState() == LoanState.APPROVED) {
        throw new LoanException("ALREADY_APPROVED");
    }
    long expectedVersion = loan.getVersion();
    LoanState expectedState = loan.getState();
    loan.approve(request.actorId(), request.comment(), clock.instant());
    if (applicationMapper.updateApproval(
            loan, expectedState, expectedVersion) != 1) {
        throw new LoanException("CONCURRENT_MODIFICATION");
    }

    LoanOperation operation = LoanOperation.approved(
        loan, request.identifier(), request.actorId(), fingerprint);
    if (operationMapper.insert(operation) != 1) {
        throw new LoanException("OPERATION_SAVE_FAILED");
    }
    return operation.toResponse("SUCCESS");
}
```

权限在返回重复结果前检查，避免凭幂等键获取无权访问的数据。摘要应包括影响结果的关键载荷，并统一空值、编码和规范化规则；不能用任意对象的无序 JSON 当作稳定指纹。示例只有备注，其他字段若进入业务必须扩展摘要和校验。

领域服务不在此方法内部捕获写入异常转为正常返回。它由独立 Spring Bean 的公开方法暴露，Facade 通过注入的代理调用；同类内部自调用不会自动获得同样的代理事务语义。

## 6. Mapper 与数据库约束

主表包含 tenant_id、application_id、state、version、approver_id、approval_comment、approved_at；操作表包含 tenant_id、application_id、event_type、identifier、actor_id、request_hash、operation_id、result_state、created_at。

主表应有 `(tenant_id, application_id)` 唯一键，操作表应有 `(tenant_id, application_id, event_type, identifier)` 唯一键。以下是 SQL 结构说明，字段类型和方言按目标数据库确定：

```sql
UPDATE loan_application
SET state = 'APPROVED', approver_id = :approverId,
    approval_comment = :comment, approved_at = :approvedAt,
    version = version + 1
WHERE tenant_id = :tenantId AND application_id = :applicationId
  AND state = :expectedState AND version = :expectedVersion;

INSERT INTO loan_operation
  (tenant_id, application_id, event_type, identifier, actor_id,
   request_hash, operation_id, result_state, created_at)
VALUES
  (:tenantId, :applicationId, 'APPROVE', :identifier, :actorId,
   :requestHash, :operationId, 'APPROVED', :createdAt);
```

UPDATE 影响行数必须为 1。操作表插入失败时抛异常，使之前 UPDATE 回滚。唯一约束仍是必须的数据库防线，不能只依靠先查重。

两个相同请求并发时，落后请求可能先遇到版本冲突或唯一冲突，不能保证第一次并发响应就是 DUPLICATED；它可以用同一 identifier 重试，提交成功后会读到原结果。若产品要求立即返回重复成功，应在事务回滚完成后重新查询并核对载荷，再转换结果，不能在失败事务中继续写。

## 7. Facade 在事务之后整理响应

```text
Controller：可信身份 + Param → ApproveLoanRequest
  → Facade.approve
    → 事务代理进入 LoanManageService.approve
      → 加载 → 授权 → 幂等 → 实体.approve
      → 条件 UPDATE → INSERT 操作结果
    → 事务提交或异常回滚
  → Facade 转换业务异常或成功结果
→ HTTP / RPC 响应
```

Facade 捕获领域异常并转换业务错误码时，事务服务已经正常提交或已回滚；Facade 不使用外层事务包住整段异常捕获。提交阶段异常同样应以失败处理，不能仅凭领域方法曾返回对象就报告成功。

## 8. 失败与重复结果

| 情况 | 结果和数据变化 |
|---|---|
| 正常批准 | APPROVED，新增成功操作记录，一起提交 |
| 同键、同操作者、同载荷重试 | 返回原 operationId 与 DUPLICATED，不新增写入 |
| 同键不同载荷/操作者 | IDEMPOTENCY_CONFLICT，不写入 |
| 不同键批准已批准申请 | ALREADY_APPROVED，不重复推进 |
| 无权限 / 不存在 | 对应业务失败，不泄漏其他租户记录 |
| 驳回或取消后的批准 | INVALID_STATE，不修改对象 |
| 与取消/另一次审批竞争失败 | CONCURRENT_MODIFICATION，回滚本次事务 |
| UPDATE 成功，流水写入失败 | 回滚 UPDATE，对外失败 |

## 9. 必要验证

实体测试验证合法迁移、非法状态时字段不变。服务测试验证授权、同键重复、同键不同载荷和已完成对象再次操作。数据库集成测试验证唯一键、两个事务的版本竞争，以及 UPDATE 成功后流水失败的回滚。

事务回滚测试应经过 Spring 代理并在事务结束后重新读取数据；只对普通 Java 对象调用方法或 Mock Mapper，不能证明数据库回滚。未执行的测试在交付中说明，不声称本说明书片段已经验证这些运行行为。

## 10. 扩展到跨业务流程

批准后若还需预占设备，增加 ApplicationService 调用资源公开能力，明确预占失败、超时反查和本地审批结果如何协调；不在实体里新增远程调用。需要可靠通知时，在提交本地结果的事务内记录通知任务，提交后由执行器投递并处理重复与重试。若业务接受普通内存事件的保证，也可采用更简单方案并如实说明。

将该案例迁移到采购、报销或任务处理时，保留职责和一致性设计方法，重新定义业务状态、权限与失败语义，不复制设备借用的规则。
