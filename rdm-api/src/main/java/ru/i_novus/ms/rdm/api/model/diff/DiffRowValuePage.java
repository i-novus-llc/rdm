package ru.i_novus.ms.rdm.api.model.diff;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria.PAGE_SHIFT;

public class DiffRowValuePage extends PageImpl<DiffRowValue> {

    public DiffRowValuePage(List<DiffRowValue> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public static DiffRowValuePage valueOf(DataPage<DiffRowValue> dataPage) {

        return new DiffRowValuePage(
                toContent(dataPage.getCollection()),
                toPageable(dataPage.getCriteria()),
                dataPage.getCount()
        );
    }

    private static ArrayList<DiffRowValue> toContent(Collection<DiffRowValue> collection) {
        return collection != null ? new ArrayList<>(collection) : new ArrayList<>();
    }

    private static PageRequest toPageable(DataCriteria criteria) {
        return PageRequest.of(criteria.getPage() - PAGE_SHIFT, criteria.getSize());
    }
}