package ru.i_novus.ms.rdm.n2o.strategy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.n2o.strategy.draft.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseUiStrategyLocatorTest {

    @Mock
    private DefaultFindOrCreateDraftStrategy defaultFindOrCreateDraftStrategy;
    @Mock
    private UnversionedFindOrCreateDraftStrategy unversionedFindOrCreateDraftStrategy;

    @Mock
    private DefaultValidateIsDraftStrategy defaultValidateIsDraftStrategy;

    /**
     * Случай:
     * Стратегия для UNVERSIONED задана и не совпадает со стратегией для DEFAULT.
     */
    @Test
    public void testGetStrategyWhenBothPresent() {

        BaseUiStrategyLocator locator = new BaseUiStrategyLocator(getStrategies());

        FindOrCreateDraftStrategy defaultStrategy =
                locator.getStrategy(RefBookTypeEnum.DEFAULT, FindOrCreateDraftStrategy.class);
        FindOrCreateDraftStrategy unversionedStrategy =
                locator.getStrategy(RefBookTypeEnum.UNVERSIONED, FindOrCreateDraftStrategy.class);

        assertNotNull(defaultStrategy);
        assertNotNull(unversionedStrategy);
        assertNotEquals(defaultStrategy, unversionedStrategy);
    }

    /**
     * Случай:
     * Стратегия для UNVERSIONED не задана, используется стратегия для DEFAULT.
     */
    @Test
    public void testGetStrategyWhenDefaultOnly() {

        BaseUiStrategyLocator locator = new BaseUiStrategyLocator(getStrategies());

        ValidateIsDraftStrategy defaultStrategy =
                locator.getStrategy(RefBookTypeEnum.DEFAULT, ValidateIsDraftStrategy.class);
        ValidateIsDraftStrategy unversionedStrategy =
                locator.getStrategy(RefBookTypeEnum.UNVERSIONED, ValidateIsDraftStrategy.class);

        assertNotNull(defaultStrategy);
        assertNotNull(unversionedStrategy);
        assertEquals(defaultStrategy, unversionedStrategy);
    }

    /**
     * Случай:
     * Стратегии для DEFAULT и для UNVERSIONED не заданы.
     */
    @Test
    public void testGetStrategyWhenBothAbsent() {

        // Т.к. нет 3-й интерфейса стратегии, вместо getStrategies() заглушка:
        BaseUiStrategyLocator locator = new BaseUiStrategyLocator(new HashMap<>());

        FindOrCreateDraftStrategy defaultStrategy =
                locator.getStrategy(RefBookTypeEnum.DEFAULT, FindOrCreateDraftStrategy.class);
        FindOrCreateDraftStrategy unversionedStrategy =
                locator.getStrategy(RefBookTypeEnum.UNVERSIONED, FindOrCreateDraftStrategy.class);

        assertNull(defaultStrategy);
        assertNull(unversionedStrategy);
    }

    private Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> getStrategies() {

        Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());
        result.put(RefBookTypeEnum.UNVERSIONED, getUnversionedStrategies());

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getDefaultStrategies() {

        Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        result.put(FindOrCreateDraftStrategy.class, defaultFindOrCreateDraftStrategy);
        result.put(ValidateIsDraftStrategy.class, defaultValidateIsDraftStrategy);

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getUnversionedStrategies() {

        Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        result.put(FindOrCreateDraftStrategy.class, unversionedFindOrCreateDraftStrategy);

        return result;
    }
}