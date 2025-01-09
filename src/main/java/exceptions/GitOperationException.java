package exceptions;

public class GitOperationException extends Exception{
    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitOperationException(String message) {
        super(message);
    }
}
