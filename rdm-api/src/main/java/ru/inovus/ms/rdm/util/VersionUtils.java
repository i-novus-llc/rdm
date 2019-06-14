package ru.inovus.ms.rdm.util;

import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.RefBookVersion;

public class VersionUtils {

    private VersionUtils() {
    }

    /**
     * Проверка статуса версии на DRAFT.
     *
     * @param version версия
     * @return Результат проверки
     */
    public static boolean isDraft(RefBookVersion version) {
        return RefBookVersionStatus.DRAFT.equals(version.getStatus());
    }
}
