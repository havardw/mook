package mook;


import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api")
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
    @Path("oidc-login")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response verifyoicd(OidcLogin oidc) {
        try {
            AuthenticationData auth = authService.verifyOidc(oidc.accessToken());
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
        if (authService.isAuthenticated(data.token())) {
            return Response.ok().entity(authService.getAuthenticationData(data.token())).build();
        } else {
            AuthenticationError error = new AuthenticationError(AuthenticationError.SESSION_EXPIRED);
            return Response.status(Response.Status.UNAUTHORIZED).entity(error).build();
        }
    }

    @POST
    @Path("logout")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response logout(SessionData data) {
        authService.logout(data.token());
        return Response.ok().build();
    }

    private static AuthenticationError fromException(AuthenticationException e) {
        return switch (e.getReason()) {
            case PASSWORD_MISMATCH -> new AuthenticationError(AuthenticationError.PASSWORD_MISMATCH);
            case OAUTH_NOT_REGISTERED ->
                    new AuthenticationError(AuthenticationError.OAUTH_NOT_REGISTERED, e.getEmail());
            case OAUTH_CONFIG -> new AuthenticationError(AuthenticationError.OAUTH_CONFIG_ERROR);
            default -> new AuthenticationError(AuthenticationError.UNKNOWN_ERROR);
        };
    }
}
