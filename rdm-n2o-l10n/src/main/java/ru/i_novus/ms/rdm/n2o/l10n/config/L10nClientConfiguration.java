package ru.i_novus.ms.rdm.n2o.l10n.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.i_novus.ms.rdm.rest.client.config.l10n.L10nRestClientConfiguration;

@Configuration
@Import(L10nRestClientConfiguration.class)
public class L10nClientConfiguration {
}
