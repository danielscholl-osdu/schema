package org.opengroup.osdu.schema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@ComponentScan({ "org.opengroup" })
public class SchemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaApplication.class, args);
    }

}
