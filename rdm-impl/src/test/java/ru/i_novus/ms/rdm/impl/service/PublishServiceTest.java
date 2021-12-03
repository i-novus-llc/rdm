package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.service.ReferenceService;
import ru.i_novus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.publish.BasePublishStrategy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class PublishServiceTest {

    @InjectMocks
    private PublishServiceImpl service;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BasePublishStrategy basePublishStrategy;

    @Mock
    private AsyncOperationQueue asyncQueue;

    @Before
    public void setUp() throws NoSuchFieldException {

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());
        FieldSetter.setField(service, PublishServiceImpl.class.getDeclaredField("strategyLocator"), strategyLocator);
    }

    @Test
    public void testPublish() {

        // to-do.
    }

    @Test
    public void testPublishWhenInvalidId() {

        Integer draftId = 0;
        try {
            service.publish(draftId, new PublishRequest());
            fail();

        } catch (UserException e) {
            assertEquals("draft.not.found", e.getCode());
            assertEquals(draftId, e.getArgs()[0]);
        }
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // RefBook:
        result.put(BasePublishStrategy.class, basePublishStrategy);

        return result;
    }
}