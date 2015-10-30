package jp.aira813.hipchatnotifier.exception;

public class HipchatNotifierException extends Exception {
    public HipchatNotifierException() {
        super();
    }

    public HipchatNotifierException(String message, Throwable cause) {
        super(message, cause);
    }

    public HipchatNotifierException(String message) {
        super(message);
    }

    public HipchatNotifierException(Throwable cause) {
        super(cause);
    }

}
