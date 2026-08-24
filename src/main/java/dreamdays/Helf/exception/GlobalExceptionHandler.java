package dreamdays.Helf.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 서비스 계층에서 던지는 예외를 적절한 HTTP 상태코드로 변환한다.
 * 이게 없으면 UserNotFoundException 같은 "정상적인" 예외도
 * 전부 500 Internal Server Error로 나가서 프론트에서 원인을 구분할 수 없다.
 *
 * 같은 상태코드(예: 409)를 여러 케이스가 공유하는 경우가 있어서,
 * 프론트가 message 텍스트가 아니라 errorCode(고정 문자열)로 분기할 수 있게
 * errorCode 필드를 같이 내려준다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 존재하지 않는 유저 조회 → 404
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", e.getMessage());
    }

    // 이미 존재하는 유저 재등록 시도 → 409
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException e) {
        return buildResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", e.getMessage());
    }

    // 이미 뽑기를 진행한 유저 → 409
    @ExceptionHandler(AlreadyDrawnException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyDrawn(AlreadyDrawnException e) {
        return buildResponse(HttpStatus.CONFLICT, "ALREADY_DRAWN", e.getMessage());
    }

    // 뽑을 수 있는 상대가 없는 경우 → 409
    @ExceptionHandler(NoMatchingUserException.class)
    public ResponseEntity<Map<String, Object>> handleNoMatchingUser(NoMatchingUserException e) {
        return buildResponse(HttpStatus.CONFLICT, "NO_MATCHING_USER", e.getMessage());
    }

    // 존재하지 않는 경로/정적 리소스 요청 (예: "/" 루트, 오타 URL 등) → 404, 에러 로그는 남기지 않음
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 경로를 찾을 수 없습니다.");
    }

    // 위 케이스로 분류되지 않은 기타 상태 오류 → 409
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
    }

    // 잘못된 요청 파라미터(예: enum 값 오타 등) → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    // 그 외 예상 못 한 예외는 로그를 남기고 500으로 응답하되, 메시지는 노출하지 않는다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "errorCode", errorCode,
                "message", message == null ? status.getReasonPhrase() : message
        );
        return ResponseEntity.status(status).body(body);
    }
}
