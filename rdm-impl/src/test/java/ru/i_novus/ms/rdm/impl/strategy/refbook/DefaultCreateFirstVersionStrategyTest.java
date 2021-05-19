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
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
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
    public void testCreate() {

        when(versionRepository.save(any(RefBookVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookEntity refBookEntity = createRefBookEntity();
        Map<String, String> passport = createRefBookPassport();

        RefBookCreateRequest request = new RefBookCreateRequest(refBookEntity.getCode(),
                RefBookTypeEnum.DEFAULT, "category", passport);
        RefBookVersionEntity entity = strategy.create(request, refBookEntity, "storage_code");

        assertEquals(refBookEntity, entity.getRefBook());
        assertEquals(RefBookVersionStatus.DRAFT, entity.getStatus());
        assertTrue(StringUtils.isEmpty(entity.getVersion()));
        assertTrue(entity.hasEmptyStructure());

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
    public void testCreateWithoutPassport() {

        when(versionRepository.save(any(RefBookVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookEntity refBookEntity = createRefBookEntity();

        RefBookCreateRequest request = new RefBookCreateRequest(refBookEntity.getCode(),
                RefBookTypeEnum.DEFAULT, "category", null);
        RefBookVersionEntity entity = strategy.create(request, refBookEntity, "storage_code");

        assertEquals(refBookEntity, entity.getRefBook());
        assertTrue(entity.hasEmptyStructure());
        assertEquals(RefBookVersionStatus.DRAFT, entity.getStatus());

        assertNull(entity.getPassportValues());
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode("test_code");

        return refBookEntity;
    }

    private Map<String, String> createRefBookPassport() {

        Map<String, String> passport = new HashMap<>(1);
        passport.put(REFBOOK_NAME_KEY, REFBOOK_NAME_VALUE);

        return passport;
    }
}