package mook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Rest endpoint for client config.
 */
@Path("/api/config.js")
public class ConfigController {


    @ConfigProperty(name = "google.clientId")
    String googleClientId;

    @ConfigProperty(name = "google.targetUrl")
    String googleTargetUrl;

    @GET
    @Produces("application/javascript")
    public Response config() {
        Map<String, Object> map = new HashMap<>();
        map.put("googleId", googleClientId);
        map.put("googleTargetUrl", googleTargetUrl);
    
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
    
        return Response.ok(result)
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
    }
}
