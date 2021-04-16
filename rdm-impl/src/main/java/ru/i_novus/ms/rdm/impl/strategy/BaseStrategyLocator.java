package ru.i_novus.ms.rdm.impl.strategy;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;

import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class BaseStrategyLocator implements StrategyLocator {

    private final Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> strategiesMap;

    public BaseStrategyLocator(Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> strategiesMap) {
        this.strategiesMap = strategiesMap;
    }

    @Override
    public <T extends Strategy> T getStrategy(RefBookType refBookType, Class<T> strategy) {

        T result = findStrategy(refBookType, strategy);

        if (result == null && !RefBookType.DEFAULT.equals(refBookType)) {
            result = findStrategy(RefBookType.DEFAULT, strategy);
        }

        return result;
    }

    private <T extends Strategy> T findStrategy(RefBookType refBookType, Class<T> strategy) {

        Map<Class<? extends Strategy>, Strategy> strategies = strategiesMap.get(refBookType);
        return strategies != null ? (T) strategies.get(strategy) : null;
    }
}
