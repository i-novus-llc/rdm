package ru.i_novus.ms.rdm.api.model.refdata;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.ArrayList;

public class RowValuePage extends PageImpl<RowValue> {

    public RowValuePage(CollectionPage<RowValue> content) {
        super(new ArrayList<>(content.getCollection()),
                PageRequest.of(content.getCriteria().getPage() - DataCriteria.PAGE_SHIFT,
                        content.getCriteria().getSize()),
                content.getCount());
    }
}
