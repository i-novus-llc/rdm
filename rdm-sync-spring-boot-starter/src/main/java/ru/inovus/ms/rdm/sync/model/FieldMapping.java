package ru.inovus.ms.rdm.sync.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
@Getter
@Setter
@AllArgsConstructor
public class FieldMapping {
    private String sysField;
    private String sysDataType;
    private String rdmField;
    private String rdmDataType;
}
