package mook;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigControllerTest {

    @Test
    public void config() throws Exception {
        Properties config = new Properties();
        config.put("mook.prefix", "test");
        config.put("mook.name", "Test");
        config.put("secret.password", "filtered");

        ConfigController controller = new ConfigController(config);

        String result = controller.config();

        assertThat(result).startsWith("var mookConfig = {");
        assertThat(result).contains("\"name\":\"Test\"");
        assertThat(result).contains("\"prefix\":\"test\"");
    }

    @Test
    public void defaultConfig() throws Exception {
        ConfigController controller = new ConfigController(new Properties());
        String result = controller.config();

        assertThat(result).contains("\"name\":\"Mook\"");
        assertThat(result).contains("\"prefix\":\"default\"");
    }
}