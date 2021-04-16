package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.api.util.VersionNumberStrategy;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedCreateFirstVersionStrategyTest {

    @InjectMocks
    private UnversionedCreateFirstVersionStrategy strategy;

    @Mock
    private VersionNumberStrategy versionNumberStrategy;

    @Test
    public void testCreate() {

        final String firstVersion = "1.0";
        when(versionNumberStrategy.first()).thenReturn(firstVersion);

        final String refBookCode = "test_code";

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode(refBookCode);

        RefBookCreateRequest request = new RefBookCreateRequest(refBookCode, RefBookType.UNVERSIONED, "category", null);
        RefBookVersionEntity entity = strategy.create(refBookEntity, request);

        assertEquals(refBookEntity, entity.getRefBook());
        assertEquals(RefBookVersionStatus.PUBLISHED, entity.getStatus());
        assertFalse(StringUtils.isEmpty(entity.getVersion()));
        assertNotNull(entity.getFromDate());

        assertNull(entity.getPassportValues());

        verify(versionNumberStrategy).first();
        verifyNoMoreInteractions(versionNumberStrategy);
    }
}