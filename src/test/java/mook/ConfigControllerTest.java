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

        Map<String, Object> result = controller.config();

        assertThat(result).containsEntry("name", "Test");
        assertThat(result).containsEntry("prefix", "test");
    }

    @Test
    public void defaultConfig() throws Exception {
        ConfigController controller = new ConfigController(new Properties());
        Map<String, Object> result = controller.config();
        assertThat(result).containsEntry("name", "Mook");
        assertThat(result).containsEntry("prefix", "default");
    }
}