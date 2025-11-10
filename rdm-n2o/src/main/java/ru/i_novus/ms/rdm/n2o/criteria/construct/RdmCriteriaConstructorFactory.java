package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.CriteriaConstructor;
import net.n2oapp.framework.api.data.CriteriaConstructorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Deprecated(since = "after update to n2o framework 7.29.6")
public class RdmCriteriaConstructorFactory extends CriteriaConstructorFactory {

    private final Map<Class<?>, CriteriaConstructor<?>> constructors;

    @Autowired
    public RdmCriteriaConstructorFactory(List<CriteriaConstructor<?>> constructorList) {
        super(constructorList);

        this.constructors = constructorList.stream()
                .collect(Collectors.toMap(CriteriaConstructor::getCriteriaClass, c -> c));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T construct(N2oPreparedCriteria criteria, T instance) {

        Class<?> criteriaClass = instance.getClass();
        CriteriaConstructor<T> constructor = null;
        while (criteriaClass != null) {
            constructor = (CriteriaConstructor<T>) constructors.get(criteriaClass);
            if (constructor != null)
                break;

            criteriaClass = criteriaClass.getSuperclass();
        }

        if (constructor == null) return instance;
        return constructor.construct(criteria, instance);
    }
}
