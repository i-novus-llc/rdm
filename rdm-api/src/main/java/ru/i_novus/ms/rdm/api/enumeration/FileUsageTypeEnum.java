package ru.i_novus.ms.rdm.api.enumeration;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Тип использования файла-справочника с допустимыми расширениями.
 *
 */
public enum FileUsageTypeEnum {

    REF_BOOK(singletonList("XML")),     // Создание справочника
    REF_DRAFT(List.of("XML", "XLSX")),  // Создание черновика
    REF_DATA(List.of("XML", "XLSX"))    // Загрузка записей
    ;

    private final List<String> extensions;

    FileUsageTypeEnum(List<String> extensions) {
        this.extensions = extensions;
    }

    public List<String> getExtensions() {
        return extensions;
    }
}
