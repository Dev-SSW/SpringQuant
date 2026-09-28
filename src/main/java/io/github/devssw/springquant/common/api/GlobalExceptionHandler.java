package io.github.devssw.springquant.common.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return badRequest("요청값이 올바르지 않습니다.", request, fieldErrors);
    }

    // JSON 문법 오류, enum 값 오류, null을 기본형 숫자로 변환하는 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return badRequest("요청 본문의 형식 또는 값이 올바르지 않습니다.", request, Map.of());
    }

    // 기간 관계, 설정값 조합 등 생성자·서비스 검증 실패
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return badRequest(exception.getMessage(), request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> badRequest(String message, HttpServletRequest request, Map<String, String> fieldErrors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(status).body(response);
    }
}