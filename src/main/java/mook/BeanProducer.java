package mook;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@ApplicationScoped
public class BeanProducer {

    @ConfigProperty(name = "mook.image.threads", defaultValue = "2")
    int thumbnailThreads;


    @Singleton
    @Named("thumbnailExecutor")
    @ConfigProperty
    ExecutorService thumbnailExecutor() {
        return Executors.newFixedThreadPool(thumbnailThreads);
    }

}