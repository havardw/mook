package mook;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationPath("api")
public class MookApplication extends ResourceConfig {

    private final AuthenticationService authenticationService;

    private final ScheduledExecutorService scheduler;

    @Inject
    public MookApplication(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        scheduler = Executors.newScheduledThreadPool(1);
        scheduleCleanup();

        packages("mook");
        register(new JacksonFeature());
    }

    private void scheduleCleanup() {
        scheduler.scheduleAtFixedRate(authenticationService::cleanExpiredTokens, 0, 1, TimeUnit.DAYS);
    }
}