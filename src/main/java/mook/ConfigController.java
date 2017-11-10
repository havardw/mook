package mook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Rest endpoint for client config.
 */
@Path("config.js")
public class ConfigController {

    private final Properties config;

    @Inject
    public ConfigController(@Named("config") Properties config) {
        this.config = config;
    }

    @GET
    @Produces("application/javascript")
    public String config() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", config.getProperty("mook.name", "Mook"));
        map.put("prefix", config.getProperty("mook.prefix", "default"));

        StringWriter stringWriter = new StringWriter();
        JsonFactory factory = new JsonFactory(new ObjectMapper());
        JsonGenerator generator;
        String result = "var mookConfig = ";
        try {
            generator = factory.createGenerator(stringWriter);
            generator.writeObject(map);
            generator.close();
            result += stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException("IOException with StringWriter should be impossible");
        }

        return result;
    }
}
