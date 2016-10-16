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
            return Response.status(Response.Status.UNAUTHORIZED).build();
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
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
}

