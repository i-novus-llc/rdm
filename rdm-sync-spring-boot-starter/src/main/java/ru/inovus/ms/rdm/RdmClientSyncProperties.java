package ru.inovus.ms.rdm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lgalimova
 * @since 20.02.2019
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "rdm.client.sync")
public class RdmClientSyncProperties {
    private String url;
}
