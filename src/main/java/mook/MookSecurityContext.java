package mook;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

class MookSecurityContext implements SecurityContext {

    private final MookPrincipal principal;

    public MookSecurityContext(MookPrincipal principal) {
        this.principal = principal;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    // Roles not implemented yet
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    // Don't care
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return "token";
    }
}