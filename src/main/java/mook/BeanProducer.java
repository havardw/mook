package mook;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@Slf4j
public class BeanProducer {
    
    @Produces
    @Singleton
    DataSource sql() {
        try {
            InitialContext ic = new InitialContext();
            return (DataSource)ic.lookup("java:comp/env/jdbc/MookDS");
        } catch (NamingException e) {
            throw new RuntimeException("DataSource lookup failed");
        }
    }
}