package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.criteria.api.SortingDirection;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс-резолвер для RestCriteria.
 */
@Component
@Order()
public class RestCriteriaConstructResolver implements CriteriaConstructResolver {

    @Override
    public boolean isSatisfied(Object instance) {
        return (instance instanceof RestCriteria);
    }

    @Override
    public void resolve(Object instance, N2oPreparedCriteria criteria) {

        RestCriteria result = ((RestCriteria) instance);

        result.setPageNumber(criteria.getPage() - 1);
        result.setPageSize(criteria.getSize());
        result.setOrders(toSortOrders(criteria));
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

    protected Sort.Direction toSortDirection(SortingDirection direction) {

        if (direction.equals(SortingDirection.ASC))
            return Sort.Direction.ASC;

        if (direction.equals(SortingDirection.DESC))
            return Sort.Direction.DESC;

        return null;
    }

    protected String toSortProperty(String field) {
        return field;
    }
}
