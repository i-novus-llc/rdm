package ru.i_novus.ms.rdm.n2o.strategy;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

public interface UiStrategyLocator {

    /**
     * Поиск стратегии по его интерфейсу для указанного типа справочника.
     *
     * @param refBookType тип справочника
     * @param strategy    интерфейс требуемой стратегии
     * @param <T>         тип интерфейса
     * @return Объект требуемой стратегии
     */
    <T extends UiStrategy> T getStrategy(RefBookTypeEnum refBookType, Class<T> strategy);
}
