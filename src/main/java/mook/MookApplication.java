package mook;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("api")
public class MookApplication extends ResourceConfig {

    public MookApplication() {
        packages("mook");
        register(new JacksonFeature());
    }
}