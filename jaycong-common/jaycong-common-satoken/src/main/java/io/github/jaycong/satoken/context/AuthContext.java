package io.github.jaycong.satoken.context;

import io.github.jaycong.satoken.model.LoginUser;
import java.util.Optional;

/** 当前请求的登录及角色鉴权入口；资源归属等业务授权仍由业务负责。 */
public interface AuthContext {
    /**
     * 获取登录资料快照；无登录身份或请求上下文时返回空。
     * @return 当前用户的登录快照
     * @throws cn.dev33.satoken.exception.NotLoginException 登录有效但资料缺失或损坏
     * @throws io.github.jaycong.satoken.exception.AuthContextUnavailableException 会话存储不可用
     */
    Optional<LoginUser> currentUser();

    /**
     * 获取有效登录及其资料快照；昵称和角色不是实时用户数据。
     * @return 当前用户的登录快照
     * @throws cn.dev33.satoken.exception.NotLoginException 未登录或资料失效
     * @throws io.github.jaycong.satoken.exception.AuthContextUnavailableException 会话存储不可用
     */
    LoginUser requireUser();

    /**
     * 仅校验登录并读取用户 ID，兼容尚未保存资料快照的旧会话。
     * @return 当前用户 ID
     * @throws cn.dev33.satoken.exception.NotLoginException 未登录或缺少请求上下文
     * @throws io.github.jaycong.satoken.exception.AuthContextUnavailableException 会话存储不可用
     */
    long requireUserId();

    /**
     * 核验有效登录以及 User 服务中的实时角色，不使用登录快照角色授权。
     * @param role 必须具备的角色，不为空白
     * @return 通过核验的当前用户 ID
     * @throws cn.dev33.satoken.exception.NotLoginException 未登录或缺少请求上下文
     * @throws io.github.jaycong.core.exception.BusinessException 账号失效为 401，角色不符为 403，核验不可用为 503
     * @throws IllegalArgumentException role 为 null 或空白
     */
    long requireRole(String role);

    /**
     * 核验当前账号具备实时 admin 角色，不提供额外权限绕过。
     * @return 通过核验的管理员 ID
     * @throws cn.dev33.satoken.exception.NotLoginException 未登录或缺少请求上下文
     * @throws io.github.jaycong.core.exception.BusinessException 账号失效为 401，无权限为 403，核验不可用为 503
     */
    default long requireAdmin() {
        return requireRole("admin");
    }

    /**
     * 判断会话是否登录，不代表快照存在或具有业务权限。
     * @return 无有效会话或请求上下文时为 false
     * @throws io.github.jaycong.satoken.exception.AuthContextUnavailableException 会话存储不可用
     */
    boolean isLogin();
}
