package ru.i_novus.ms.rdm.impl.model;

import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

/**
 * Набор сущностей-версий справочника:
 * неизменяемая и изменяемая версии.
 */
public class RefBookVersionEntityKit {

    /** Сущность: (последняя) опубликованная версия. */
    private final RefBookVersionEntity publishedEntity;

    /** Сущность: версия-черновик. */
    private final RefBookVersionEntity draftEntity;

    public RefBookVersionEntityKit(RefBookVersionEntity publishedEntity,
                                   RefBookVersionEntity draftEntity) {
        this.publishedEntity = publishedEntity;
        this.draftEntity = draftEntity;
    }

    public RefBookVersionEntity getPublishedEntity() {
        return publishedEntity;
    }

    public RefBookVersionEntity getDraftEntity() {
        return draftEntity;
    }

    public RefBookEntity getRefBook() {

        if (publishedEntity != null)
            return publishedEntity.getRefBook();

        return (draftEntity != null) ? draftEntity.getRefBook() : null;
    }
}
