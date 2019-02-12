package ru.inovus.ms.rdm.criteria;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.CriteriaConstructor;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RestCriteriaConstructor implements CriteriaConstructor, Serializable {


    @Override
    public <T> T construct(N2oPreparedCriteria criteria, Class<T> criteriaClass) {

        T instance;
        try {
            instance = criteriaClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
        if (instance instanceof RestCriteria) {
            List<Order> sortings = new ArrayList<>();
            for (Sorting sorting : criteria.getSortings()) {
                sortings.add(toOrder(sorting));
            }
            ((RestCriteria) instance).setOrders(sortings);
            ((RestCriteria) instance).setPageSize(criteria.getSize());
            ((RestCriteria) instance).setPageNumber(criteria.getPage() - 1);
        }else if (instance instanceof Criteria) {
            ((Criteria) instance).setSorting(criteria.getSorting());
            ((Criteria) instance).setPage(criteria.getPage());
            ((Criteria) instance).setSize(criteria.getSize());
        }
        return instance;
    }

    private Order toOrder(Sorting sorting) {
        Sort.Direction direction = null;
        if (sorting.getDirection().equals(Direction.ASC))
            direction = Sort.Direction.ASC;
        if (sorting.getDirection().equals(Direction.DESC))
            direction = Sort.Direction.DESC;
        if (("name").equals(sorting.getField()))
            return new Order(direction, "passport." + sorting.getField());
        else
            return new Order(direction, sorting.getField());
    }
}
