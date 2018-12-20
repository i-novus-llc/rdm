package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;

import java.util.List;

public class RefBookRowValue extends LongRowValue {
    private String id;


    public RefBookRowValue() {
    }

    public RefBookRowValue(Long systemId, List<FieldValue> fieldValues, String id) {
        super(systemId, fieldValues);
        this.id = id;
    }

    public RefBookRowValue(String id, FieldValue... fieldValues) {
        super(fieldValues);
        this.id = id;
    }

    public RefBookRowValue(LongRowValue rowValue, Integer versionId) {
        super(rowValue.getSystemId(), rowValue.getFieldValues(), rowValue.getHash());
        this.id = rowValue.getHash() + "$" + versionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
