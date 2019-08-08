package ru.inovus.ms.rdm.model;

/**
 * Created by znurgaliev on 07.11.2018.
 */
public enum RefBookStatus {
    // Состояние справочника:
    ARCHIVED,       // Архивный

    // Состояние версии справочника:
    HAS_DRAFT,      // В работе (есть черновик)
    PUBLISHED       // Опубликован
}
