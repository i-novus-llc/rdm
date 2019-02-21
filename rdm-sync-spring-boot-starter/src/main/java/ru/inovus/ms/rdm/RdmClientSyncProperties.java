package ru.inovus.ms.rdm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@ConfigurationProperties(prefix = "rdm.client.sync")
public class RdmClientSyncProperties {

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
