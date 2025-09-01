package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.criteria.api.SortingDirectionEnum;
import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.processing.DataProcessing;
import net.n2oapp.framework.api.ui.QueryRequestInfo;
import net.n2oapp.framework.api.ui.QueryResponseInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataRecordSortingProcessing implements DataProcessing {

    private static final String QUERY_ID = "data";
    private static final String SORTING_KEY = "sorting";

    @Override
    public void processQuery(QueryRequestInfo requestInfo, QueryResponseInfo responseInfo) {

        if (!QUERY_ID.equals(requestInfo.getQuery().getId()) ||
                !requestInfo.getData().containsKey(SORTING_KEY))
            return;

        final DataSet sortingSet = (DataSet) requestInfo.getData().get(SORTING_KEY);
        if (sortingSet.isEmpty())
            return;

        final List<Sorting> sortings = sortingSet.entrySet().stream().map(this::toSorting).toList();
        requestInfo.getCriteria().setSortings(sortings);
    }

    private Sorting toSorting(Map.Entry<String, Object> entry) {

        return new Sorting(entry.getKey(), SortingDirectionEnum.valueOf((String) entry.getValue()));
    }
}
