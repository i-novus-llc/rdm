package ru.i_novus.ms.rdm.impl.strategy;

public class BaseStrategyLocator implements StrategyLocator {

    @Override
    public <T> T getStrategy(String refBookType, Class<T> strategy) {
        return null;
    }
}
