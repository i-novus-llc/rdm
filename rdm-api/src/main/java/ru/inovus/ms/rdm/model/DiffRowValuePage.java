package ru.inovus.ms.rdm.model;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.ArrayList;

public class DiffRowValuePage extends PageImpl<DiffRowValue> {

    public DiffRowValuePage(CollectionPage<DiffRowValue> content) {
        super(content.getCollection() != null
                        ? new ArrayList(content.getCollection())
                        : new ArrayList<>(),
                new PageRequest(content.getCriteria().getPage() - 1,
                        content.getCriteria().getSize()),
                content.getCount());
    }

}