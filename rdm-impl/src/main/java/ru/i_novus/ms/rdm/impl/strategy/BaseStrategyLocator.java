package ru.i_novus.ms.rdm.impl.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.strategy.refbook.*;

@Component
@SuppressWarnings("unchecked")
public class BaseStrategyLocator implements StrategyLocator {

    @Autowired
    private DefaultRefBookCreateValidationStrategy defaultRefBookCreateValidationStrategy;

    @Autowired
    private DefaultCreateFirstDataDraftStrategy defaultCreateFirstDataDraftStrategy;

    @Autowired
    private UnversionedCreateFirstDataDraftStrategy unversionedCreateFirstDataDraftStrategy;

    @Override
    public <T extends Strategy> T getStrategy(RefBookType refBookType, Class<T> strategy) {

        return (RefBookType.UNVERSIONED == refBookType)
                ? getUnversionedStrategy(strategy)
                : getDefaultStrategy(strategy);
    }

    protected <T extends Strategy> T getDefaultStrategy(Class<T> strategy) {

        if (strategy == null)
            throw new IllegalArgumentException("Strategy interface is not specified");

        if (RefBookCreateValidationStrategy.class == strategy)
            return (T) defaultRefBookCreateValidationStrategy;

        else if (CreateRefBookEntityStrategy.class == strategy)
            return (T) new DefaultCreateRefBookEntityStrategy();

        else if (CreateFirstVersionStrategy.class == strategy)
            return (T) new DefaultCreateFirstVersionStrategy();

        else if (CreateFirstDataDraftStrategy.class == strategy)
            return (T) defaultCreateFirstDataDraftStrategy;

        throw new IllegalArgumentException(String.format("Strategy for %s is not found", strategy.getSimpleName()));
    }

    public <T extends Strategy> T getUnversionedStrategy(Class<T> strategy) {

        if (CreateFirstVersionStrategy.class == strategy)
            return (T) new UnversionedCreateFirstVersionStrategy();

        else if (CreateFirstDataDraftStrategy.class == strategy)
            return (T) unversionedCreateFirstDataDraftStrategy;

        return getDefaultStrategy(strategy);
    }
}
