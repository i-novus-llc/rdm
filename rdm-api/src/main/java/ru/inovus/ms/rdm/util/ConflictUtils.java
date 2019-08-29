package ru.inovus.ms.rdm.util;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.inovus.ms.rdm.enumeration.ConflictType;

public class ConflictUtils {

    private ConflictUtils() {
    }

    public static ConflictType diffStatusToConflictType(DiffStatusEnum diffStatus) {
        if (diffStatus == null)
            return null;

        if (diffStatus.equals(DiffStatusEnum.DELETED))
            return ConflictType.DELETED;

        if (diffStatus.equals(DiffStatusEnum.UPDATED))
            return ConflictType.UPDATED;

        return null;
    }

    public static DiffStatusEnum conflictTypeToDiffStatus(ConflictType conflictType) {
        if (conflictType == null)
            return null;

        if (conflictType.equals(ConflictType.DELETED))
            return DiffStatusEnum.DELETED;

        if (conflictType.equals(ConflictType.UPDATED)
                || conflictType.equals(ConflictType.ALTERED))
            return DiffStatusEnum.UPDATED;

        return null;
    }
}
