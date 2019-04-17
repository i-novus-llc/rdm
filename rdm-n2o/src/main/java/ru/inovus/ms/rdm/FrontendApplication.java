package ru.inovus.ms.rdm;

import net.n2oapp.framework.config.register.scanner.XmlInfoScanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FrontendApplication {

    @Bean
    public XmlInfoScanner myInfoScanner() {
        return new XmlInfoScanner("classpath*:/access/**/*.xml");
    }

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
