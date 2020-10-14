package ru.i_novus.ms.rdm.loader.client;

import net.n2oapp.platform.loader.autoconfigure.ClientLoaderConfigurer;
import net.n2oapp.platform.loader.client.ClientLoaderRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.loader.client.loader.RefBookDataClientLoader;

/**
 * Настройщик загрузчиков файлов справочников RDM.
 */
@Configuration
@ConditionalOnProperty(value = "rdm.loader.client.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RefBookDataClientLoaderProperties.class)
public class RefBookDataClientLoaderConfigurer implements ClientLoaderConfigurer {

    private static final String RDM_TARGET = "refBookData";
    
    @Autowired
    private RefBookDataClientLoaderProperties properties;

    @Override
    public void configure(ClientLoaderRunner runner) {
        runner.add(properties.getUrl(), properties.getSubject(), RDM_TARGET,
                properties.getFilePath(), RefBookDataClientLoader.class);
    }
}
