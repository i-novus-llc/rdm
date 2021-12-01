package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.CriteriaConstructor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

public class RestCriteriaConstructor implements CriteriaConstructor {

    private final List<CriteriaConstructResolver> resolvers;

    public RestCriteriaConstructor(Collection<CriteriaConstructResolver> resolvers) {

        this.resolvers = new ArrayList<>(resolvers);
        this.resolvers.sort(AnnotationAwareOrderComparator.INSTANCE); // Сортировка по @Order
    }

    @Override
    public <T> T construct(N2oPreparedCriteria criteria, Class<T> criteriaClass) {

        T instance = newInstance(criteriaClass);
        prepareInstance(criteria, instance);
        return instance;
    }

    protected <T> T newInstance(Class<T> criteriaClass) {
        try {
            return criteriaClass.getDeclaredConstructor().newInstance();

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected <T> void prepareInstance(N2oPreparedCriteria criteria, T instance) {

        CriteriaConstructResolver resolver = getSatisfiedResolver(instance);
        if (resolver == null)
            return;

        resolver.resolve(instance, criteria);
    }

    private <T> CriteriaConstructResolver getSatisfiedResolver(T instance) {

        if (isEmpty(resolvers))
            return null;

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(instance))
                .findFirst().orElse(null);
    }
}
