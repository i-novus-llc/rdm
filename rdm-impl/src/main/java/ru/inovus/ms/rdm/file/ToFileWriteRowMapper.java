package ru.inovus.ms.rdm.file;

import ru.i_novus.platform.datastorage.temporal.model.Reference;

/**
 * Created by znurgaliev on 24.07.2018.
 */
public class ToFileWriteRowMapper implements RowMapper {

    @Override
    public Row map(Row inputRow) {
        inputRow.getData().entrySet().forEach(entry -> {
            if (entry.getValue() instanceof Reference){
                entry.setValue(((Reference)entry.getValue()).getValue());
            }
        });
        return inputRow;
    }
}
