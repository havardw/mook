package mook;

/**
 * Represents a site with its permission for a user.
 */
public record SitePermission(String name, String path, AuthenticationData.Permission permission) {
}