package ru.inovus.ms.rdm.util;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.Conflict;

public class ConflictUtils {

    private ConflictUtils() {
    }

    public static ConflictType diffStatusToConflictType(DiffStatusEnum diffStatus) {
        if (diffStatus == null)
            return null;

        return diffStatus.equals(DiffStatusEnum.DELETED)
                ? ConflictType.DELETED
                : ConflictType.UPDATED;
    }

    public static DiffStatusEnum conflictTypeToDiffStatus(ConflictType conflictType) {
        if (conflictType == null)
            return null;

        return conflictType.equals(ConflictType.DELETED)
                ? DiffStatusEnum.DELETED
                : DiffStatusEnum.UPDATED;
    }

    /**
     * Проверка типа конфликта на UPDATED.
     *
     * @param conflict конфликт
     * @return Результат проверки
     */
    public static boolean isUpdatedConflict(Conflict conflict) {
        return ConflictType.UPDATED.equals(conflict.getConflictType());
    }
}
