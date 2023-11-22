package ru.i_novus.ms.rdm.api.model.refdata;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.ArrayList;

public class RowValuePage extends PageImpl<RowValue> {

    public RowValuePage(DataPage<RowValue> content) {
        super(new ArrayList<>(content.getCollection()),
                PageRequest.of(content.getCriteria().getPage() - DataCriteria.PAGE_SHIFT,
                        content.getCriteria().getSize()),
                content.getCount());
    }
}
