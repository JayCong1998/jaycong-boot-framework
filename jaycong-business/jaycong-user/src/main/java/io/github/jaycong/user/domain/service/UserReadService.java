package io.github.jaycong.user.domain.service;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.datasource.pagination.MybatisPages;
import io.github.jaycong.satoken.context.AuthContext;
import io.github.jaycong.user.UserException;
import io.github.jaycong.user.domain.entity.User;
import io.github.jaycong.user.infrastructure.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** 用户及会员读取直接使用 Mapper；管理读取按本服务的实时账号状态鉴权。 */
@Service
public class UserReadService {
    private final UserMapper mapper;
    private final AuthContext authContext;
    private final StpLogic sessions;

    public UserReadService(UserMapper mapper, AuthContext authContext, StpLogic sessions) {
        this.mapper = mapper;
        this.authContext = authContext;
        this.sessions = sessions;
    }

    public User findEnabledUser(long id) {
        var user = mapper.selectById(id);
        if (user == null) {
            throw new UserException(401, "未登录或登录已失效");
        }
        user.requireEnabled();
        return user;
    }

    /** account 已由用户实体规范化；未知账号仍交给密码校验执行占位哈希比对。 */
    public User findByAccount(String account) {
        var query = Wrappers.<User>lambdaQuery();
        if (account.contains("@")) {
            query.eq(User::getEmail, account);
        } else {
            query.eq(User::getMobile, account);
        }
        return mapper.selectOne(query);
    }

    public PageResult<User> listMembers(int page, int size, String keyword, Long id, Integer status,
            LocalDateTime start, LocalDateTime end) {
        requireAdmin();
        if (page < 1 || size < 1 || size > 100 || (status != null && status != 0 && status != 1)
                || (start != null && end != null && start.isAfter(end))) {
            throw new UserException(400, "分页、状态或时间范围无效");
        }
        String term = keyword == null ? "" : keyword.strip();
        if (term.length() > 254) {
            throw new UserException(400, "搜索内容过长");
        }
        var query = Wrappers.<User>lambdaQuery().eq(User::getRole, "user")
                .eq(id != null, User::getId, id).eq(status != null, User::getStatus, status)
                .ge(start != null, User::getCreateTime, start)
                .le(end != null, User::getCreateTime, end);
        if (!term.isEmpty()) {
            // 搜索按字面包含匹配，不能把用户输入当作 LIKE 通配符。
            String literal = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            query.and(q -> q.like(User::getNickname, literal)
                    .or().like(User::getMobile, literal).or().like(User::getEmail, literal));
        }
        query.orderByDesc(User::getCreateTime, User::getId);
        var result = mapper.selectPage(MybatisPages.toPage(new PageRequest(page, size)), query);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    public User memberDetail(long id) {
        requireAdmin();
        return getMember(id);
    }

    /** 供已完成授权的管理用例使用，管理员账号不作为普通会员处理。 */
    public User getMember(long id) {
        var user = mapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getId, id).eq(User::getRole, "user"));
        if (user == null) {
            throw new UserException(404, "会员不存在");
        }
        user.requireMember();
        return user;
    }

    /** User 是账号数据来源，直接本地核验，避免通过 RPC 回调自身。 */
    public User requireAdmin() {
        long id = authContext.requireUserId();
        User user;
        try {
            user = findEnabledUser(id);
        } catch (UserException exception) {
            if (exception.getStatus() == 401) {
                sessions.logout(id);
            }
            throw exception;
        }
        if (!"admin".equals(user.getRole())) {
            throw new UserException(403, "仅管理员可执行此操作");
        }
        return user;
    }
}
