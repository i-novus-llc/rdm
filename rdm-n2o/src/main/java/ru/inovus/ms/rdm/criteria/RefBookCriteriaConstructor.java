package ru.inovus.ms.rdm.criteria;

import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.CriteriaConstructor;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RefBookCriteriaConstructor implements CriteriaConstructor, Serializable {


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
            Sort.Direction direction = null;
            for (Sorting sorting : criteria.getSortings()) {
                switch (sorting.getDirection()) {
                    case ASC: {
                        direction = Sort.Direction.ASC;
                        break;
                    }
                    case DESC: {
                        direction = Sort.Direction.DESC;
                        break;
                    }
                }
                if (sorting.getField().equals("name"))
                    sortings.add(new Order(direction, "passport." + sorting.getField()));
                else
                    sortings.add(new Order(direction,  sorting.getField()));
            }
            ((RestCriteria) instance).setOrders(sortings);
            ((RestCriteria) instance).setPageSize(criteria.getSize());
            ((RestCriteria) instance).setPageNumber(criteria.getPage() - 1);
        }
        return instance;
    }
}
