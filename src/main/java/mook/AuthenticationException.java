package mook;

public class AuthenticationException extends RuntimeException {

    private final Reason reason;

    public AuthenticationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        UNKNOWN,
        PASSWORD_MISMATCH,
        OAUTH_NOT_REGISTERED,
        OAUTH_CONFIG,
        SESSION_EXPIRED,
        NETWORK
    }
}