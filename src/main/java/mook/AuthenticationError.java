package mook;
public record AuthenticationError(String errorCode, String email) {
    public static final String OAUTH_NOT_REGISTERED   = "oauth.not.registered";
    public static final String OAUTH_CONFIG_ERROR     = "oauth.config.error";
    public static final String PASSWORD_MISMATCH      = "password.mismatch";
    public static final String SESSION_EXPIRED        = "session.expired";
    public static final String UNKNOWN_ERROR          = "unknown.error";

    public AuthenticationError(String errorCode) {
        this(errorCode, null);
    }
}
