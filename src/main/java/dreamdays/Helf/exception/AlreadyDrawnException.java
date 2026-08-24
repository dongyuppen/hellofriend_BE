package dreamdays.Helf.exception;

public class AlreadyDrawnException extends RuntimeException {

    public AlreadyDrawnException() {
        super();
    }

    public AlreadyDrawnException(String message) {
        super(message);
    }

    public AlreadyDrawnException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyDrawnException(Throwable cause) {
        super(cause);
    }
}
