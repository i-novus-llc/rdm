package ru.inovus.ms.rdm.util.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.n2oapp.platform.jaxrs.MapperConfigurer;

import java.time.LocalDateTime;

/**
 * @author arahmatullin
 * since 14.05.2019
 */
public class RdmN2oLocalDateTimeMapperPreparer implements MapperConfigurer {

    @Override
    public void configure(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(new RdmN2oJsonLocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new RdmN2oJsonLocalDateTimeDeserializer());
        objectMapper.registerModule(module);
    }
}
