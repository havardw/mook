package mook;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Slf4j
public class BeanProducer {

    @Produces
    @Singleton
    DataSource sql() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getRequiredProperty("mook.db.url"));
        config.setUsername(getRequiredProperty("mook.db.username"));
        config.setPassword(getRequiredProperty("mook.db.password"));

        return new HikariDataSource(config);
    }

    private static String getRequiredProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(String.format("Required property %1$s not set, use -D%1$s=... on command line", key));
        }

        return value;
    }
}