package ru.i_novus.ms.rdm.api.model;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class AbstractCriteria extends RestCriteria {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int NO_PAGINATION_SIZE = Integer.MAX_VALUE;

    public AbstractCriteria() {
        this(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }

    public AbstractCriteria(int pageNumber, int pageSize) {
        super(pageNumber, pageSize);
    }

    public AbstractCriteria(RestCriteria criteria) {
        this(criteria.getPageNumber(), criteria.getPageSize());
    }

    @Override
    protected List<Sort.Order> getDefaultOrders() {
        return Collections.emptyList();
    }

    public void makeUnpaged() {
        setPageSize(NO_PAGINATION_SIZE);
        setPageNumber(DEFAULT_PAGE_NUMBER);
    }

    public List<Sort.Order> getOrders() {
        return getSort().get().collect(toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractCriteria that = (AbstractCriteria) o;
        return Objects.equals(getPageNumber(), that.getPageNumber()) &&
                Objects.equals(getPageSize(), that.getPageSize()) &&
                Objects.equals(getSort(), that.getSort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getPageNumber(), getPageSize(), getSort());
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
