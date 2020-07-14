package ru.i_novus.ms.rdm.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@ConfigurationProperties(prefix = "rdm.client.sync")
public class RdmClientSyncProperties {
    private static final String RDM_DEFAULT_URL = "http://docker.one:8807/rdm/api";

    private String url = RDM_DEFAULT_URL;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
