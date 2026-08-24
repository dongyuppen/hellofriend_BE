package dreamdays.Helf.exception;

public class NoMatchingUserException extends RuntimeException {

    public NoMatchingUserException() {
        super();
    }

    public NoMatchingUserException(String message) {
        super(message);
    }

    public NoMatchingUserException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoMatchingUserException(Throwable cause) {
        super(cause);
    }
}
