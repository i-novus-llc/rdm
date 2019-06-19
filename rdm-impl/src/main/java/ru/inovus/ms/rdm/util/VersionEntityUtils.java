package ru.inovus.ms.rdm.util;

import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;

public class VersionEntityUtils {

    private VersionEntityUtils() {
    }

    /**
     * Проверка статуса версии на DRAFT.
     *
     * @param versionEntity версия
     * @return Результат проверки
     */
    public static boolean isDraft(RefBookVersionEntity versionEntity) {
        return RefBookVersionStatus.DRAFT.equals(versionEntity.getStatus());
    }
}
