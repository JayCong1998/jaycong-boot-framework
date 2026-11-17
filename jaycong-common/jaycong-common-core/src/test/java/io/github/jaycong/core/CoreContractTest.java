package io.github.jaycong.core;

import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.error.ErrorCode;
import io.github.jaycong.core.exception.BusinessException;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.core.model.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoreContractTest {
    private static final ErrorCode DOMAIN_ERROR = new ErrorCode() {
        public Integer getCode() { return 10002; }
        public String getMessage() { return "订单已关闭"; }
    };

    @Test
    void successPreservesDataAndSupportsNoPayload() {
        var result = Result.success("hello");
        assertEquals(0, result.code());
        assertTrue(result.isSuccess());
        assertTrue(Result.success().isSuccess());
        assertEquals("成功", result.message());
        assertEquals("hello", result.data());
        assertNull(Result.success().data());
    }

    @Test
    void failureSupportsDomainCodesAndSafeCustomMessages() {
        var result = Result.failure(DOMAIN_ERROR);
        assertEquals(10002, result.code());
        assertFalse(result.isSuccess());
        assertEquals("订单已关闭", result.message());
        assertNull(result.data());
        assertEquals("订单无法修改", Result.failure(DOMAIN_ERROR, "订单无法修改").message());
    }

    @Test
    void rejectsInvalidResponseContracts() {
        assertThrows(IllegalArgumentException.class, () -> Result.failure(CommonErrorCode.SUCCESS));
        assertThrows(IllegalArgumentException.class, () -> new Result<>(400, " ", null));
        assertThrows(NullPointerException.class, () -> Result.failure(null));
        assertThrows(NullPointerException.class, () -> new Result<>(null, "message", null));
        assertThrows(NullPointerException.class, () -> new Result<>(400, null, null));
    }

    @Test
    void businessExceptionRetainsDomainCodeMessageAndCause() {
        var cause = new IllegalStateException("internal detail");
        var exception = new BusinessException(DOMAIN_ERROR, "无法修改订单", cause);
        assertEquals(10002, exception.getCode());
        assertEquals("无法修改订单", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("订单已关闭", new BusinessException(DOMAIN_ERROR).getMessage());
        assertEquals("自定义消息", new BusinessException(DOMAIN_ERROR, "自定义消息").getMessage());
        assertThrows(IllegalArgumentException.class, () -> new BusinessException(CommonErrorCode.SUCCESS));
        assertThrows(IllegalArgumentException.class, () -> new BusinessException(DOMAIN_ERROR, " "));
        assertThrows(NullPointerException.class, () -> new BusinessException(null));
    }

    @Test
    void paginationUsesOneBasedPagesAndLongOffsets() {
        assertEquals(new PageRequest(1, 20), new PageRequest());
        assertEquals(0L, new PageRequest().offset());
        assertEquals(40L, new PageRequest(3, 20).offset());
        assertEquals(2_147_483_646_000L, new PageRequest(Integer.MAX_VALUE, 1000).offset());
    }

    @Test
    void invalidPaginationIsRejectedRatherThanSilentlyAdjusted() {
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 20));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(1, -1));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(1, 1001));
    }

    @Test
    void pageRecordsAreIsolatedFromCallerMutation() {
        var source = new ArrayList<>(List.of("one"));
        var result = new PageResult<>(21L, source);
        source.clear();
        assertEquals(List.of("one"), result.rows());
        assertThrows(UnsupportedOperationException.class, () -> result.rows().clear());
        assertEquals(21L, result.total());
    }

    @Test
    void emptyPagesPreserveZeroOrLargeTotals() {
        assertEquals(0L, new PageResult<>(0, List.of()).total());
        assertEquals(Long.MAX_VALUE, new PageResult<>(Long.MAX_VALUE, List.of("one")).total());
        assertTrue(new PageResult<>(21, List.of()).rows().isEmpty());
    }

    @Test
    void rejectsInvalidPageResults() {
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(-1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(1, List.of(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(0, List.of(1)));
        assertThrows(NullPointerException.class, () -> new PageResult<>(0, null));
        var nullItem = new ArrayList<String>();
        nullItem.add(null);
        assertThrows(NullPointerException.class, () -> new PageResult<>(1, nullItem));
    }

    @Test
    void directConstructionDerivesSuccessFromNumericCode() {
        assertTrue(new Result<>(0, "ok", null).isSuccess());
        assertFalse(new Result<>(400, "bad request", null).isSuccess());
        assertFalse(new Result<>(10002, "domain error", null).isSuccess());
    }

    @Test
    void responseRetainsValueEquality() {
        var first = Result.success("hello");
        var second = new Result<>(0, "成功", "hello");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, Result.success("other"));
    }
}
