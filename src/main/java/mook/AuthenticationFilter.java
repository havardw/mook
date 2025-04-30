package mook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final AuthenticationService authService;
    
    private final List<String> skipPaths = Arrays.asList("/api/login", "/api/oidc-login", "/api/resumeSession", "/api/config.js");

    @Inject
    public AuthenticationFilter(AuthenticationService authService) {
        this.authService = authService;
    }

    @Override
    public void filter(ContainerRequestContext context) {

        String path = context.getUriInfo().getPath();
        
        if (!skipPaths.contains(path)) {
            String auth = context.getHeaderString("auth");
            if (auth != null && !auth.isEmpty()) {
                if (authService.isAuthenticated(auth)) {
                    context.setSecurityContext(new MookSecurityContext(authService.getPrincipal(auth)));
                } else {
                    log.info("Not authenticated");
                    sendUnauthorized(context, "Expired or invalid session");
                }
            } else {
                log.info("No header set");
                sendUnauthorized(context, "No authentication in request");
            }
        }        
    }
    
    private void sendUnauthorized(ContainerRequestContext context, String reason) {
        // Send as HTML, else Angular will have problems getting status
        context.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.TEXT_HTML_TYPE).entity(reason).build());
    }
}