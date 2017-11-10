package mook;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.*;

/**
 * Rest endpoint for client config.
 */
@Path("config")
public class ConfigController {

    private final Properties config;

    @Inject
    public ConfigController(@Named("config") Properties config) {
        this.config = config;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> config() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", config.getProperty("mook.name", "Mook"));
        result.put("prefix", config.getProperty("mook.prefix", "default"));
        return result;
    }
}
