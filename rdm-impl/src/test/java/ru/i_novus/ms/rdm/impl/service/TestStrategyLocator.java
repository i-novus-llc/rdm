package ru.i_novus.ms.rdm.impl.service;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public class TestStrategyLocator extends BaseStrategyLocator {

    @Override
    public <T extends Strategy> T getStrategy(RefBookType refBookType, Class<T> strategy) {
        return getDefaultStrategy(strategy);
    }
}
