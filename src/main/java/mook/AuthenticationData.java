package mook;
import java.util.List;
import java.util.UUID;

public record AuthenticationData(int id, String email, String displayName, UUID token, List<SitePermission> sitePermissions) {
    public enum Permission {
        ADMIN,
        EDIT
    }
}
