package ru.inovus.ms.rdm.api.util;

import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Paginate<C extends AbstractCriteria, E> {

    private C criteria;
    private int pageSize = 10;
    private Function<? super C, Page<? extends E>> supplier;

    private Paginate(C criteria) {
        this.criteria = criteria;
    }

    public Paginate<C, E> withPageSupply(Function<? super C, Page<? extends E>> supplier) {
        this.supplier = supplier;
        return this;
    }

    public void forEach(Consumer<? super E> handle) {
        go(e -> {
            handle.accept(e);
            return false;
        });
    }

    @SuppressWarnings("squid:S1452")
    public Optional<? extends E> findOneSuchThat(Predicate<? super E> findOne) {
        final Optional<? extends E>[] optional = new Optional[]{Optional.empty()};
        go(e -> {
            if (findOne.test(e)) {
                optional[0] = Optional.ofNullable(e);
                return true;
            }
            return false;
        });
        return optional[0];
    }

    public Paginate<C, E> pageSize(int pageSize) {
        if (pageSize <= 0)
            throw new IllegalArgumentException();
        this.pageSize = pageSize;
        return this;
    }

    private void go(Function<? super E, Boolean> handle) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(handle);
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
        return new Paginate<>(Objects.requireNonNull(criteria));
    }

}
