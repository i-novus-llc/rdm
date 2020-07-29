package ru.i_novus.ms.rdm.n2o;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(value = {ClientConfiguration.class})
@ComponentScan(basePackages = "ru.i_novus.ms.rdm.n2o")
public class RdmWebConfiguration {
}
