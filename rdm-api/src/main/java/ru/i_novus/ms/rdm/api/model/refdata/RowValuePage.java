package ru.i_novus.ms.rdm.api.model.refdata;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria.PAGE_SHIFT;

public class RowValuePage extends PageImpl<RowValue> {

    public RowValuePage(List<RowValue> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public static RowValuePage valueOf(DataPage<RowValue> dataPage) {

        return new RowValuePage(
                toContent(dataPage.getCollection()),
                toPageable(dataPage.getCriteria()),
                dataPage.getCount()
        );
    }

    private static ArrayList<RowValue> toContent(Collection<RowValue> collection) {
        return collection != null ? new ArrayList<>(collection) : new ArrayList<>();
    }

    private static PageRequest toPageable(DataCriteria criteria) {
        return PageRequest.of(criteria.getPage() - PAGE_SHIFT, criteria.getSize());
    }
}
