package ru.inovus.ms.rdm.util;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ConflictUtils {

    private static final List<ConflictType> DATA_CONFLICT_TYPES = asList(
            ConflictType.UPDATED, ConflictType.DELETED, ConflictType.ALTERED
    );

    private static final List<ConflictType> STRUCTURE_CONFLICT_TYPES = singletonList(
            ConflictType.DISPLAY_DAMAGED
    );

    private ConflictUtils() {
    }

    public static List<ConflictType> getDataConflictTypes() {
        return DATA_CONFLICT_TYPES;
    }

    public static List<ConflictType> getStructureConflictTypes() {
        return STRUCTURE_CONFLICT_TYPES;
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
