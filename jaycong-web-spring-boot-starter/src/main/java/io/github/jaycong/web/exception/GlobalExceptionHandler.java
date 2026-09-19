package io.github.jaycong.web.exception;

import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.exception.BusinessException;
import io.github.jaycong.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** MVC 异常统一响应；保留协议状态与响应头，不暴露内部异常详情。 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.unprocessableEntity()
                .body(new Result<>(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled request exception", exception);
        return ResponseEntity.internalServerError().body(Result.failure(CommonErrorCode.SYSTEM_ERROR));
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        Result<Void> result;
        if (statusCode.is5xxServerError()) {
            log.error("Request processing failed with status {}", statusCode.value(), exception);
            result = Result.failure(CommonErrorCode.SYSTEM_ERROR);
        } else if (statusCode.value() == 400) {
            result = Result.failure(CommonErrorCode.INVALID_ARGUMENT);
        } else if (statusCode.value() == 404) {
            result = Result.failure(CommonErrorCode.NOT_FOUND);
        } else {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            result = new Result<>(statusCode.value(),
                    status == null ? "请求失败" : status.getReasonPhrase(), null);
        }
        return super.handleExceptionInternal(exception, result, headers, statusCode, request);
    }
}
