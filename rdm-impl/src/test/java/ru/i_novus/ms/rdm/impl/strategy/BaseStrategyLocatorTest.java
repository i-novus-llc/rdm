package ru.i_novus.ms.rdm.impl.strategy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.BaseTest;
import ru.i_novus.ms.rdm.impl.strategy.refbook.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseStrategyLocatorTest extends BaseTest {

    @Mock
    private DefaultRefBookCreateValidationStrategy defaultRefBookCreateValidationStrategy;
    @Mock
    private UnversionedCreateFirstVersionStrategy unversionedCreateFirstVersionStrategy;

    @Mock
    private DefaultCreateFirstVersionStrategy defaultCreateFirstVersionStrategy;

    /**
     * Случай:
     * Стратегия для UNVERSIONED задана и не совпадает со стратегией для DEFAULT.
     */
    @Test
    public void testGetStrategyWhenBothPresent() {

        BaseStrategyLocator locator = new BaseStrategyLocator(getStrategies());

        CreateFirstVersionStrategy defaultStrategy =
                locator.getStrategy(RefBookType.DEFAULT, CreateFirstVersionStrategy.class);
        CreateFirstVersionStrategy unversionedStrategy =
                locator.getStrategy(RefBookType.UNVERSIONED, CreateFirstVersionStrategy.class);

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

        BaseStrategyLocator locator = new BaseStrategyLocator(getStrategies());

        RefBookCreateValidationStrategy defaultStrategy =
                locator.getStrategy(RefBookType.DEFAULT, RefBookCreateValidationStrategy.class);
        RefBookCreateValidationStrategy unversionedStrategy =
                locator.getStrategy(RefBookType.UNVERSIONED, RefBookCreateValidationStrategy.class);

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

        BaseStrategyLocator locator = new BaseStrategyLocator(getStrategies());

        CreateFirstStorageStrategy defaultStrategy =
                locator.getStrategy(RefBookType.DEFAULT, CreateFirstStorageStrategy.class);
        CreateFirstStorageStrategy unversionedStrategy =
                locator.getStrategy(RefBookType.UNVERSIONED, CreateFirstStorageStrategy.class);

        assertNull(defaultStrategy);
        assertNull(unversionedStrategy);
    }

    private Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookType.DEFAULT, getDefaultStrategies());
        result.put(RefBookType.UNVERSIONED, getUnversionedStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        result.put(RefBookCreateValidationStrategy.class, defaultRefBookCreateValidationStrategy);
        result.put(CreateFirstVersionStrategy.class, defaultCreateFirstVersionStrategy);

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getUnversionedStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        result.put(CreateFirstVersionStrategy.class, unversionedCreateFirstVersionStrategy);

        return result;
    }
}