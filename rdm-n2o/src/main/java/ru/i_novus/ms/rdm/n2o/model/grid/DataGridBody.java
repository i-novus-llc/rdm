package ru.i_novus.ms.rdm.n2o.model.grid;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.n2oapp.framework.api.metadata.meta.cell.AbstractCell;
import net.n2oapp.framework.api.metadata.meta.cell.TextCell;

import java.util.List;

/**
 * Тело DataGrid: Конфигурация колонок.
 */
@Getter
@RequiredArgsConstructor
@SuppressWarnings("WeakerAccess")
public class DataGridBody {

    @JsonProperty
    private final List<AbstractCell> cells;

    public static AbstractCell createTextCell(String id) {

        final TextCell cell = new TextCell();
        cell.setId(id);
        cell.setFieldKey(id);
        cell.setSrc("TextCell");

        return cell;
    }
}
