package ru.i_novus.ms.rdm.impl.strategy;

import java.io.Serializable;

public interface StrategyLocator extends Serializable {

    <T> T getStrategy(String refBookType, Class<T> strategy);
}
