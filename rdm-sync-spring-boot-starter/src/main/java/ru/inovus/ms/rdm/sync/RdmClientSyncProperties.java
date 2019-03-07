package ru.inovus.ms.rdm.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@ConfigurationProperties(prefix = "rdm.client.sync")
public class RdmClientSyncProperties {
    public static final String DEFAULT_URL = "http://docker.one:8807/rdm/api";

    private String url = DEFAULT_URL;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
