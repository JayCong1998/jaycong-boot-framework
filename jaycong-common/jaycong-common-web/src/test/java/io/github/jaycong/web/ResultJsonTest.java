package io.github.jaycong.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.core.model.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultJsonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void failureSerializesFalseAndNumericCode() throws Exception {
        var json = mapper.readTree(mapper.writeValueAsString(Result.failure(CommonErrorCode.INVALID_ARGUMENT)));
        assertThat(json).isEqualTo(mapper.readTree("""
                {"success":false,"code":400,"message":"参数错误","data":null}
                """));
    }

    @Test
    void serializesNumericCodeSuccessAndOnlyTwoPaginationFields() throws Exception {
        var response = Result.success(new PageResult<>(21, List.of("last")));
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertThat(json).isEqualTo(mapper.readTree("""
                {"success":true,"code":0,"message":"成功","data":{"total":21,"rows":["last"]}}
                """));
    }

    @Test
    void emptyPageSerializesAsArrayWithLongTotal() throws Exception {
        var json = mapper.readTree(mapper.writeValueAsString(new PageResult<>(Long.MAX_VALUE, List.of())));
        assertThat(json).isEqualTo(mapper.readTree("""
                {"total":9223372036854775807,"rows":[]}
                """));
    }
}
