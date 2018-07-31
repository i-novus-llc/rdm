package ru.inovus.ms.rdm;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ClientConfiguration {
    @Value("${rdm.backend.path}")
    private String restUrl;

    @Bean
    public JacksonJsonProvider cxfJsonProvider() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule clientBackendModule = new SimpleModule("ClientBackendModule", Version.unknownVersion());
        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
        resolver.addMapping(Page.class, RestPage.class);
        clientBackendModule.setAbstractTypes(resolver);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(clientBackendModule);

        JavaTimeModule jtm = new JavaTimeModule();
        jtm.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(TimeUtils.DATE_TIME_PATTERN_FORMATTER));
        mapper.registerModule(jtm);

        return new JacksonJsonProvider(mapper);
    }

    @Bean
    public RefBookService refBookService(@Qualifier("cxfJsonProvider") JacksonJsonProvider cxfJsonProvider) {
        return createClient(cxfJsonProvider, RefBookService.class);
    }

    @Bean
    public VersionService versionService(@Qualifier("cxfJsonProvider") JacksonJsonProvider cxfJsonProvider) {
        return createClient(cxfJsonProvider, VersionService.class);
    }

    @Bean
    public DraftService draftService(@Qualifier("cxfJsonProvider") JacksonJsonProvider cxfJsonProvider) {
        return createClient(cxfJsonProvider, DraftService.class);
    }

    private <T> T createClient(JacksonJsonProvider cxfJsonProvider, Class<T> restServiceClass) {
        List<Object> providers = new ArrayList<>();
        providers.add(cxfJsonProvider);
        return JAXRSClientFactory.create(restUrl,
                restServiceClass,
                providers);
    }
}

