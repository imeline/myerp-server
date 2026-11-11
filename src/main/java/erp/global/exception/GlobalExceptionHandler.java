package erp.global.exception;

import erp.global.response.ApiResponse;
import erp.log.dto.request.LogSaveRequest;
import erp.log.enums.LogType;
import erp.log.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
// mvc controller error 처리 + ERROR 감사 로그 기록
public class GlobalExceptionHandler {

    private final LogService logService;

    // 왜 default로 prod 프로필을 사용하지 않는지?
    // 로컬에서 prod로 연결되어 버리면 실제 운영 DB에 연결될 수 있기 때문
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 비즈니스 예외 처리 (커스텀 4xx 등)
     * - 응답 HTTP 상태코드를 반드시 세팅(AccessLogFilter가 정확히 기록)
     * - ERROR 감사 로그는 남기지 않음(정책: 4xx는 ACCESS만)
     */
    @ExceptionHandler(GlobalException.class)
    public ApiResponse<?> handleGlobalException(GlobalException ex,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        var es = ex.getStatus();
        log.warn("biz error: {}, url: {}, msg: {}", es, request.getRequestURI(), ex.getMessage());

        // 응답 HTTP 상태코드만 세팅 (ACCESS 로그가 올바른 status로 기록됨)
        response.setStatus(es.getHttpStatus().value());

        // ※ ERROR 감사 로그는 남기지 않음 (GlobalException은 ACCESS만)
        return ApiResponse.onFailure(es, ex.getMessage());
    }

    /**
     * @Valid @RequestBody 검증 실패 → 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        String message = toFieldErrorMessage(ex.getBindingResult());
        log.warn("400 BadRequest(MethodArgumentNotValid) url: {}, detail: {}", request.getRequestURI(), message);

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, message);
    }

    /**
     * @Valid @ModelAttribute, @RequestParam 검증 실패 → 400
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<?> handleBindException(BindException ex,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String message = toFieldErrorMessage(ex.getBindingResult());
        log.warn("400 BadRequest(BindException) url: {}, detail: {}", request.getRequestURI(), message);

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, message);
    }

    /**
     * field error 목록 → "field: message, field: message"
     * - 타입 변환 실패(query string의 잘못된 enum·숫자 등)는 내부 클래스명이 담긴
     *   기본 메시지 대신 고정 문구로 내려준다.
     */
    private String toFieldErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + (e.isBindingFailure() ? "올바르지 않은 값입니다." : e.getDefaultMessage()))
                .collect(Collectors.joining(", "));
    }

    /**
     * JSON 파싱 실패 / 요청 본문 읽기 실패 → 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<?> handleNotReadable(HttpMessageNotReadableException ex,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        log.warn("400 BadRequest(NotReadable) url: {}, detail: {}", request.getRequestURI(), ex.getMessage());

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return ApiResponse.onFailure(ErrorStatus.NOT_READABLE);
    }

    /**
     * 예상치 못한 모든 런타임 예외 처리 (내부 메시지 숨김)
     * - 5xx: ERROR 감사로그 1건 기록
     * - 응답도 500으로 내려서 AccessLogFilter가 status=500으로 기록
     */
    @ExceptionHandler(RuntimeException.class)
    protected ApiResponse<?> handleRuntimeException(RuntimeException ex,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        if (ex instanceof GlobalException ge) {
            return handleGlobalException(ge, request, response);
        }
        log.error("500 InternalServerError url: {}, detail: ", request.getRequestURI(), ex);

        // ERROR 감사 로그 (성공=false)
        writeErrorLog(
                "unhandled-exception",
                request,
                Map.of(
                        "error", ex.getClass().getSimpleName(),
                        "message", ex.getMessage()
                ),
                ex
        );

        boolean isProd = "prod".equalsIgnoreCase(activeProfile);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return isProd
                ? ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR)
                : ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private void writeErrorLog(String message, HttpServletRequest request, Map<String, Object> extra, Throwable ex) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("path", request.getRequestURI());
            payload.put("method", request.getMethod());
            if (request.getQueryString() != null) {
                payload.put("query", request.getQueryString());
            }
            if (extra != null && !extra.isEmpty()) {
                payload.putAll(extra);
            }
            payload.put("exception", ex.getClass().getName());

            // LogServiceImpl이 RequestContext로 ip/requestId를 채워주므로
            // 여기선 단순 메시지/페이로드만 전달
            logService.save(new LogSaveRequest(
                    LogType.ERROR,
                    false,
                    message,
                    payload
            ));
        } catch (Exception loggingEx) {
            // 감사로그 실패로 사용자 응답이 영향을 받지 않도록 방어
            log.warn("failed to write error audit log: {}", loggingEx.toString());
        }
    }
}
