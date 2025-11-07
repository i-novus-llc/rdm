package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.criteria.api.SortingDirectionEnum;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.CriteriaConstructor;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Абстрактный класс-резолвер для RestCriteria и его потомков.
 */
public abstract class RestCriteriaConstructor<T extends RestCriteria> implements CriteriaConstructor<T> {

    public T prepareInstance(N2oPreparedCriteria criteria, T instance) {

        instance.setPageNumber(criteria.getPage() - 1);
        instance.setPageSize(criteria.getSize());
        instance.setOrders(toSortOrders(criteria));

        return instance;
    }

    private List<Sort.Order> toSortOrders(N2oPreparedCriteria criteria) {

        List<Sort.Order> sortings = new ArrayList<>();
        for (Sorting sorting : criteria.getSortings()) {
            sortings.add(toSortOrder(sorting));
        }
        return sortings;
    }

    private Sort.Order toSortOrder(Sorting sorting) {

        return new Sort.Order(
                toSortDirection(sorting.getDirection()),
                toSortProperty(sorting.getField())
        );
    }

    protected Sort.Direction toSortDirection(SortingDirectionEnum direction) {

        if (direction.equals(SortingDirectionEnum.ASC))
            return Sort.Direction.ASC;

        if (direction.equals(SortingDirectionEnum.DESC))
            return Sort.Direction.DESC;

        return null;
    }

    protected String toSortProperty(String field) {
        return field;
    }
}
