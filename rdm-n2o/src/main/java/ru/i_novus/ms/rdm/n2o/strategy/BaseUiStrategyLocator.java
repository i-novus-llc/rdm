package ru.i_novus.ms.rdm.n2o.strategy;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class BaseUiStrategyLocator implements UiStrategyLocator {

    private final Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> strategiesMap;

    public BaseUiStrategyLocator(Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> strategiesMap) {
        this.strategiesMap = strategiesMap;
    }

    @Override
    public <T extends UiStrategy> T getStrategy(RefBookTypeEnum refBookType, Class<T> uiStrategy) {

        T result = findStrategy(refBookType, uiStrategy);

        if (result == null && !RefBookTypeEnum.DEFAULT.equals(refBookType)) {
            result = findStrategy(RefBookTypeEnum.DEFAULT, uiStrategy);
        }

        return result;
    }

    private <T extends UiStrategy> T findStrategy(RefBookTypeEnum refBookType, Class<T> uiStrategy) {

        Map<Class<? extends UiStrategy>, UiStrategy> strategies = strategiesMap.get(refBookType);
        return strategies != null ? (T) strategies.get(uiStrategy) : null;
    }
}
