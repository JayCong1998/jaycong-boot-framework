package io.github.jaycong.core.model;

/** 从 1 开始的分页参数；参数无效时直接拒绝，避免静默修改查询条件。 */
public record PageRequest(int page, int size) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 1000;

    public PageRequest {
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }

    public PageRequest() {
        this(1, DEFAULT_SIZE);
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}
