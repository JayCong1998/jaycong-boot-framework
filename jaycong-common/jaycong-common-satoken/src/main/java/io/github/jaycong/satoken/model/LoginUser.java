package io.github.jaycong.satoken.model;

import cn.dev33.satoken.json.SaJsonType;
import java.io.Serializable;

/** 登录身份快照；SaJsonType 允许 Sa-Token JSON 还原该类型，role 不能替代实时授权。 */
public record LoginUser(long userId, String nickname, String role) implements SaJsonType, Serializable {
}
