package mook;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@Slf4j
public class BeanProducer {

    private final InitialContext ic;

    public BeanProducer()  {
        try {
            ic = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException("Failed to create InitialContext", e);
        }
    }

    @Produces
    @Singleton
    DataSource sql() {
        try {
            return (DataSource)ic.lookup("java:comp/env/jdbc/MookDS");
        } catch (NamingException e) {
            throw new RuntimeException("DataSource lookup failed");
        }
    }

    @Produces
    @Named("imagePath")
    String imagePath() {
        try {
            return (String) ic.lookup("java:comp/env/MookImagePath");
        } catch (NamingException e) {
            throw new RuntimeException("Failed to look up image path, please define env-entry 'MookImagePath'");
        }
    }
}