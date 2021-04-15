package ru.i_novus.ms.rdm.impl.strategy;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;

public interface StrategyLocator {

    /**
     * Поиск стратегии по его интерфейсу для указанного типа справочника.
     *
     * @param refBookType тип справочника
     * @param strategy    интерфейс требуемой стратегии
     * @param <T>         тип интерфейса
     * @return Объект требуемой стратегии
     */
    <T extends Strategy> T getStrategy(RefBookType refBookType, Class<T> strategy);
}
