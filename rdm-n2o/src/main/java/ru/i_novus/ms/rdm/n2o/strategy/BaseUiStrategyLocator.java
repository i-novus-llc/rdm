package ru.i_novus.ms.rdm.n2o.strategy;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;

import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class BaseUiStrategyLocator implements UiStrategyLocator {

    private final Map<RefBookType, Map<Class<? extends UiStrategy>, UiStrategy>> strategiesMap;

    public BaseUiStrategyLocator(Map<RefBookType, Map<Class<? extends UiStrategy>, UiStrategy>> strategiesMap) {
        this.strategiesMap = strategiesMap;
    }

    @Override
    public <T extends UiStrategy> T getStrategy(RefBookType refBookType, Class<T> uiStrategy) {

        T result = findStrategy(refBookType, uiStrategy);

        if (result == null && !RefBookType.DEFAULT.equals(refBookType)) {
            result = findStrategy(RefBookType.DEFAULT, uiStrategy);
        }

        return result;
    }

    private <T extends UiStrategy> T findStrategy(RefBookType refBookType, Class<T> uiStrategy) {

        Map<Class<? extends UiStrategy>, UiStrategy> strategies = strategiesMap.get(refBookType);
        return strategies != null ? (T) strategies.get(uiStrategy) : null;
    }
}
