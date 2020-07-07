package ru.inovus.ms.rdm.sync.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
@Getter
@Setter
@AllArgsConstructor
public class VersionMapping {
    private Integer id;
    private String code;
    private String version;
    private LocalDateTime publicationDate;
    private String table;
    private String primaryField;
    private String deletedField;
    private LocalDateTime mappingLastUpdated;
    private LocalDateTime lastSync;

    public boolean changed() {
        return mappingLastUpdated.isAfter(lastSync);
    }

}
