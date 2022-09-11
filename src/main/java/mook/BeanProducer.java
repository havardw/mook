package mook;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class BeanProducer {
    
    private static final Logger log = LoggerFactory.getLogger(BeanProducer.class);

    @ConfigProperty(name = "mook.image.threads", defaultValue = "2")
    int thumbnailThreads;


    @Singleton
    @Named("thumbnailExecutor")
    @ConfigProperty
    ExecutorService thumbnailExecutor() {
        return Executors.newFixedThreadPool(thumbnailThreads);
    }

}