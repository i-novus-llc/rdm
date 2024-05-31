package ru.i_novus.ms.rdm.n2o.model.grid;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.n2oapp.framework.api.metadata.meta.cell.AbstractCell;
import net.n2oapp.framework.api.metadata.meta.cell.TextCell;
import net.n2oapp.framework.api.metadata.meta.control.Control;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import net.n2oapp.framework.api.metadata.meta.widget.table.ColumnHeader;

import java.util.List;

/**
 * Конфигурация колонок для DataGrid.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class DataGridColumnsConfig {

    @JsonProperty
    private final List<ColumnHeader> headers;

    @JsonProperty
    private final List<AbstractCell> cells;

    public static ColumnHeader createHeader(String id, String label, StandardField<Control> filterField) {

        return createHeader(id, label, filterField != null, true, true, filterField);
    }

    public static ColumnHeader createHeader(String id, String label,
                                            boolean filterable, boolean resizable, boolean sortable,
                                            StandardField<Control> filterField) {

        return createHeader(id, label, "TextTableHeader",
                filterable, resizable, sortable ? id : null, filterField);
    }

    public static ColumnHeader createHeader(String id, String label, String src,
                                            boolean filterable, boolean resizable, String sortingParam,
                                            StandardField<Control> filterField) {

        final ColumnHeader header = new ColumnHeader();

        header.setId(id);
        header.setLabel(label);
        header.setSrc(src);

        header.setFilterable(filterable);
        header.setResizable(resizable);
        header.setSortingParam(sortingParam);
        header.setFilterField(filterField);

        return header;
    }

    public static AbstractCell createTextCell(String id) {

        final TextCell cell = new TextCell();
        cell.setId(id);
        cell.setFieldKey(id);
        cell.setSrc("TextCell");

        return cell;
    }
}
