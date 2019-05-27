package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public class Conflict {

    private ConflictType conflictType;

    private List<FieldValue> primaryKeys;

}
