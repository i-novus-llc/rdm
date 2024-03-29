package ru.i_novus.ms.rdm.api.util.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.n2oapp.platform.jaxrs.MapperConfigurer;

import java.time.LocalDateTime;

/**
 * @author arahmatullin
 * since 14.05.2019
 */
public class LocalDateTimeMapperPreparer implements MapperConfigurer {

    @Override
    public void configure(ObjectMapper objectMapper) {

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new JsonLocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new JsonLocalDateTimeDeserializer());

        objectMapper.registerModule(module);
    }
}
