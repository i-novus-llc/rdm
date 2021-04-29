package ru.i_novus.ms.rdm.impl.strategy;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class BaseStrategyLocator implements StrategyLocator {

    private final Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> strategiesMap;

    public BaseStrategyLocator(Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> strategiesMap) {
        this.strategiesMap = strategiesMap;
    }

    @Override
    public <T extends Strategy> T getStrategy(RefBookTypeEnum refBookType, Class<T> strategy) {

        T result = findStrategy(refBookType != null ? refBookType : RefBookTypeEnum.DEFAULT, strategy);

        if (result == null && !RefBookTypeEnum.DEFAULT.equals(refBookType)) {
            result = findStrategy(RefBookTypeEnum.DEFAULT, strategy);
        }

        return result;
    }

    private <T extends Strategy> T findStrategy(RefBookTypeEnum refBookType, Class<T> strategy) {

        Map<Class<? extends Strategy>, Strategy> strategies = strategiesMap.get(refBookType);
        return strategies != null ? (T) strategies.get(strategy) : null;
    }
}
