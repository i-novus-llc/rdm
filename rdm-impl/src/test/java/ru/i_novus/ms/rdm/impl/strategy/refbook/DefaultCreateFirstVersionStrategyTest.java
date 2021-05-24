package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCreateFirstVersionStrategyTest {

    private static final String REFBOOK_NAME_KEY = "name";
    private static final String REFBOOK_NAME_VALUE = "Reference book";

    @InjectMocks
    private DefaultCreateFirstVersionStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testCreateDefaultWithPassport() {

        when(versionRepository.save(any(RefBookVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookEntity refBookEntity = new DefaultRefBookEntity();
        refBookEntity.setCode("test_code");

        Map<String, String> passport = new HashMap<>(1);
        passport.put(REFBOOK_NAME_KEY, REFBOOK_NAME_VALUE);

        RefBookCreateRequest request = new RefBookCreateRequest(refBookEntity.getCode(),
                RefBookTypeEnum.DEFAULT, "category", passport);
        RefBookVersionEntity entity = strategy.create(request, refBookEntity, "storage_code");

        assertEquals(refBookEntity, entity.getRefBook());
        assertTrue(entity.hasEmptyStructure());
        assertTrue(StringUtils.isEmpty(entity.getVersion()));
        assertEquals(RefBookVersionStatus.DRAFT, entity.getStatus());
        assertNull(entity.getFromDate());

        List<PassportValueEntity> passportValues = entity.getPassportValues();
        assertNotNull(passportValues);
        assertEquals(1, passportValues.size());

        PassportValueEntity passportValue = passportValues.get(0);
        assertEquals(REFBOOK_NAME_KEY, passportValue.getAttribute().getCode());
        assertEquals(REFBOOK_NAME_VALUE, passportValue.getValue());
        assertEquals(entity,passportValue.getVersion());

        verify(versionRepository).save(any(RefBookVersionEntity.class));
        verifyNoMoreInteractions(versionRepository);
    }

    @Test
    public void testCreateUnversionedWithoutPassport() {

        when(versionRepository.save(any(RefBookVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookEntity refBookEntity = new UnversionedRefBookEntity();
        refBookEntity.setCode("test_code");

        RefBookCreateRequest request = new RefBookCreateRequest(refBookEntity.getCode(),
                RefBookTypeEnum.DEFAULT, "category", null);
        RefBookVersionEntity entity = strategy.create(request, refBookEntity, "storage_code");

        assertEquals(refBookEntity, entity.getRefBook());
        assertTrue(entity.hasEmptyStructure());
        assertFalse(StringUtils.isEmpty(entity.getVersion()));
        assertEquals(RefBookVersionStatus.PUBLISHED, entity.getStatus());
        assertNotNull(entity.getFromDate());

        assertNull(entity.getPassportValues());
    }
}