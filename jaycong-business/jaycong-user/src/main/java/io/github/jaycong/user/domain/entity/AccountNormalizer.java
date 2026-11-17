package io.github.jaycong.user.domain.entity;

import io.github.jaycong.user.UserException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** 当前手机号限定中国大陆号码；不进行短信或邮箱所有权验证。 */
public final class AccountNormalizer {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");

    private AccountNormalizer() { }

    public static String mobile(String value) {
        value = optional(value);
        if (value == null) return null;
        value = value.replace(" ", "").replace("-", "");
        if (value.startsWith("+86")) value = value.substring(3);
        else if (value.startsWith("0086")) value = value.substring(4);
        if (!value.matches("1[3-9][0-9]{9}")) throw invalid("手机号格式错误");
        return value;
    }

    public static String email(String value) {
        value = optional(value);
        if (value == null) return null;
        value = value.toLowerCase(Locale.ROOT);
        if (value.length() > 254 || !EMAIL.matcher(value).matches()) throw invalid("邮箱格式错误");
        String localPart = value.substring(0, value.indexOf('@'));
        if (localPart.length() > 64 || localPart.startsWith(".") || localPart.endsWith(".")
                || localPart.contains("..")) throw invalid("邮箱格式错误");
        return value;
    }

    public static void password(String value) {
        if (value == null || value.isBlank() || value.length() < 8 || value.length() > 64)
            throw invalid("密码长度必须为8到64个字符");
        if (value.getBytes(StandardCharsets.UTF_8).length > 72) throw invalid("密码 UTF-8 长度不能超过72字节");
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public static UserException invalid(String message) {
        return new UserException(400, message);
    }
}
