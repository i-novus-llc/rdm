package ru.inovus.ms.rdm.model;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.ArrayList;

public class RowValuePage extends PageImpl<RowValue> {

    public RowValuePage(CollectionPage<RowValue> content) {
        super(new ArrayList(content.getCollection()), new PageRequest(content.getCriteria().getPage() - 1,
                content.getCriteria().getSize()), content.getCount());
    }
}
