package mook;
import java.util.Map;

public record AuthenticationData(int id, String email, String displayName, String token, Map<String, Permission> sitePermissions) {
    public enum Permission {
        ADMIN,
        EDIT
    }
}
