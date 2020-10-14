package ru.i_novus.ms.rdm.loader.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Свойства настройщика загрузчиков файлов справочников RDM.
 */
@ConfigurationProperties(prefix = "rdm.loader.client")
public class RefBookDataClientLoaderProperties {

    private static final String DEFAULT_URL = "http://docker.one:8807/rdm/api";
    private static final String DEFAULT_SUBJECT = "client";
    private static final String DEFAULT_FILE_PATH = "rdm.json";

    /** URL REST-сервиса RDM. */
    private String url = DEFAULT_URL;

    /** Владелец справочников. */
    private String subject = DEFAULT_SUBJECT;

    /** Путь к файлу конфигурации. */
    private String filePath = DEFAULT_FILE_PATH;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
