package uitgo.driverservice.exception;

public class DriverServiceException extends RuntimeException {

    private String errorCode;
    private String details;

    public DriverServiceException(String message) {
        super(message);
    }

    public DriverServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DriverServiceException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}
