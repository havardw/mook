package mook;
import java.util.List;

public record AuthenticationData(int id, String email, String displayName, String token, List<SitePermission> sitePermissions) {
    public enum Permission {
        ADMIN,
        EDIT
    }
}
