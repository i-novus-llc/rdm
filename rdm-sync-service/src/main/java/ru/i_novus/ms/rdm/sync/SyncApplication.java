package ru.i_novus.ms.rdm.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SyncAppConfig.class)
public class SyncApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SyncApplication.class, args);
    }
}
