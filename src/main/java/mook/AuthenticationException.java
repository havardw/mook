package mook;

public class AuthenticationException extends RuntimeException {

    private final Reason reason;

    private final String email;

    public AuthenticationException(Reason reason, String email) {
        super(reason.name());
        this.reason = reason;
        this.email = email;
    }

    public AuthenticationException(Reason reason) {
        this(reason, null);
    }

    public Reason getReason() {
        return reason;
    }

    public String getEmail() {
        return email;
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