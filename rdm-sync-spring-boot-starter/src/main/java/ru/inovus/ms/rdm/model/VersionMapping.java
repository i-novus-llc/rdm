package ru.inovus.ms.rdm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

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
}
