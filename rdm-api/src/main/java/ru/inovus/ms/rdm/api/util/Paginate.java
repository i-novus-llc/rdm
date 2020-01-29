package ru.inovus.ms.rdm.api.util;

import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import java.util.Objects;
import java.util.function.Function;

public class Paginate<C extends AbstractCriteria, E> {

    private C criteria;
    private int pageSize = 10;
    private Function<? super C, Page<? extends E>> supplier;
    private Function<? super E, Boolean> handle;

    private Paginate(C criteria) {
        this.criteria = criteria;
    }

    public Paginate<C, E> supplyWith(Function<? super C, Page<? extends E>> supplier) {
        this.supplier = supplier;
        return this;
    }

    public Paginate<C, E> onEachDo(Function<? super E, Boolean> handle) {
        this.handle = handle;
        return this;
    }

    public Paginate<C, E> pageSize(int pageSize) {
        if (pageSize <= 0)
            throw new IllegalArgumentException();
        this.pageSize = pageSize;
        return this;
    }

    public void go() {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(handle);
        Objects.requireNonNull(criteria);
        int page = AbstractCriteria.FIRST_PAGE_NUMBER;
        criteria.setPageNumber(page);
        criteria.setPageSize(pageSize);
        Page<? extends E> res = supplier.apply(criteria);
        long total = res.getTotalElements();
        while (total / pageSize >= page) {
            criteria.setPageNumber(++page);
            boolean done = false;
            for (E e : res.getContent()) {
                done = handle.apply(e);
                if (done)
                    break;
            }
            if (done)
                break;
            if (total / pageSize >= page)
                res = supplier.apply(criteria);
        }
    }

    public static <C extends AbstractCriteria, E> Paginate<C, E> over(C criteria) {
        return new Paginate<>(criteria);
    }

}
