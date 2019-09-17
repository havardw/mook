package mook;


import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class LoginController {

    private final AuthenticationService authService;

    @Inject
    public LoginController(AuthenticationService authService) {
        this.authService = authService;
    }

    @POST
    @Path("login")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response doLogin(LoginData data) {
        try {
            AuthenticationData auth = authService.login(data);
            return Response.ok().entity(auth).build();
        } catch (AuthenticationException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(fromException(e)).build();
        }
    }
    
    @POST
    @Path("google-id")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response verifyGoogleId(GoogleId id) {
        try {
            AuthenticationData auth = authService.verifyGoogleLogin(id.getTokenId());
            return Response.ok().entity(auth).build();
        } catch (AuthenticationException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(fromException(e)).build();
        }
    }
    
    @POST
    @Path("resumeSession")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response resumeSession(SessionData data) {
        if (authService.isAuthenticated(data.getToken())) {
            return Response.ok().entity(authService.getAuthenticationData(data.token)).build();
        } else {
            AuthenticationError error = new AuthenticationError(AuthenticationError.SESSION_EXPIRED);
            return Response.status(Response.Status.UNAUTHORIZED).entity(error).build();
        }
    }

    private static AuthenticationError fromException(AuthenticationException e) {
        switch (e.getReason()) {
            case PASSWORD_MISMATCH: return new AuthenticationError(AuthenticationError.PASSWORD_MISMATCH);
            case OAUTH_NOT_REGISTERED: return new AuthenticationError(AuthenticationError.OAUTH_NOT_REGISTERED);
            case OAUTH_CONFIG: return new AuthenticationError(AuthenticationError.OAUTH_CONFIG_ERROR);
            default:
                return new AuthenticationError(AuthenticationError.UNKNOWN_ERROR);
        }
    }
}

