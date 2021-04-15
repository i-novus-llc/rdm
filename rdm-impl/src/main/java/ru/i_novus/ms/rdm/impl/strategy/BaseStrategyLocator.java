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
    private DefaultRefBookCreateFirstDraftStrategy defaultRefBookCreateFirstDraftStrategy;

    @Autowired
    private UnversionedRefBookCreateFirstDraftStrategy unversionedRefBookCreateFirstDraftStrategy;

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

        else if (RefBookCreateEntityStrategy.class == strategy)
            return (T) new DefaultRefBookCreateEntityStrategy();

        else if (RefBookCreateVersionStrategy.class == strategy)
            return (T) new DefaultRefBookCreateVersionStrategy();

        else if (RefBookCreateFirstDraftStrategy.class == strategy)
            return (T) defaultRefBookCreateFirstDraftStrategy;

        throw new IllegalArgumentException(String.format("Strategy for %s is not found", strategy.getSimpleName()));
    }

    public <T extends Strategy> T getUnversionedStrategy(Class<T> strategy) {

        if (RefBookCreateVersionStrategy.class == strategy)
            return (T) new UnversionedRefBookCreateVersionStrategy();

        else if (RefBookCreateFirstDraftStrategy.class == strategy)
            return (T) unversionedRefBookCreateFirstDraftStrategy;

        return getDefaultStrategy(strategy);
    }
}
