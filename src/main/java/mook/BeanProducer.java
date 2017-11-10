package mook;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class BeanProducer {

    private final Properties properties;

    /**
     * Load config from file.
     */
    public BeanProducer() {
        String configFile = System.getProperty("mook.config");
        if (configFile == null) {
            throw new IllegalStateException("Path to config file not set, use '-Dmook.config=...' on command line");
        }

        File config = new File(configFile);
        if (!config.exists() || !config.canRead()) {
            throw new IllegalStateException("Config file '" + configFile + "' does not exist or is not readable");
        }

        properties = new Properties();
        try (InputStream is = new FileInputStream(config)) {
            properties.load(is);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file " + configFile);
        }
    }

    @Produces
    @Singleton
    DataSource sql() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getRequiredProperty("mook.db.url"));
        config.setUsername(getRequiredProperty("mook.db.username"));
        config.setPassword(getRequiredProperty("mook.db.password"));

        return new HikariDataSource(config);
    }

    @Produces
    @Singleton
    @Named("thumbnailExecutor")
    ExecutorService thumbnailExecutor() {
        return Executors.newFixedThreadPool(Integer.parseInt(properties.getProperty("mook.image.threads", "2")));
    }

    private String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(String.format("Required property %s not set", key));
        }

        return value;
    }

    @Produces
    @Named("imagePath")
    String imagePath() {
        return getRequiredProperty("mook.image.path");
    }

    @Produces
    @Named("config")
    Properties config() {
        return properties;
    }
}