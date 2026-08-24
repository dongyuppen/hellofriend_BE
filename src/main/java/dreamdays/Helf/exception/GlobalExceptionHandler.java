package dreamdays.Helf.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 서비스 계층에서 던지는 예외를 적절한 HTTP 상태코드로 변환한다.
 * 이게 없으면 UserNotFoundException 같은 "정상적인" 예외도
 * 전부 500 Internal Server Error로 나가서 프론트에서 원인을 구분할 수 없다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 존재하지 않는 유저 조회 → 404
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 이미 존재하는 유저 재등록 시도 → 409
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    // 이미 뽑기를 진행했거나 / 뽑을 수 있는 상대가 없는 경우 등 잘못된 상태 요청 → 409
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    // 잘못된 요청 파라미터(예: enum 값 오타 등) → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 그 외 예상 못 한 예외는 로그를 남기고 500으로 응답하되, 메시지는 노출하지 않는다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "message", message == null ? status.getReasonPhrase() : message
        );
        return ResponseEntity.status(status).body(body);
    }
}
