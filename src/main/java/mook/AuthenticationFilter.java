package mook;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

@Provider
@Slf4j
public class AuthenticationFilter implements ContainerRequestFilter {

    private final AuthenticationService authService;
    
    private final List<String> skipPaths = Arrays.asList("login", "oidc-login", "resumeSession", "config.js");

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