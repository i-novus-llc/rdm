package ru.inovus.ms.rdm.util;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

public class RowValuePage extends PageImpl<RowValue> {

    public RowValuePage(CollectionPage<RowValue> content) {
        super((List<RowValue>) content.getCollection(), new PageRequest(content.getCriteria().getPage(),
                content.getCriteria().getSize()), content.getCount());
    }
}
