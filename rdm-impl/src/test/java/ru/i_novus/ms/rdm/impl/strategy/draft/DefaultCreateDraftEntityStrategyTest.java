package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCreateDraftEntityStrategyTest {

    private static final String REFBOOK_NAME_KEY = "name";
    private static final String REFBOOK_NAME_VALUE = "Reference book";

    @InjectMocks
    private DefaultCreateDraftEntityStrategy strategy;

    @Test
    public void testCreate() {

        RefBookEntity refBookEntity = createRefBookEntity();
        RefBookVersionEntity entity = strategy.create(refBookEntity, new Structure(), createPassportValues());

        assertEquals(refBookEntity, entity.getRefBook());
        assertTrue(entity.hasEmptyStructure());
        assertEquals(RefBookVersionStatus.DRAFT, entity.getStatus());

        List<PassportValueEntity> passportValues = entity.getPassportValues();
        assertNotNull(passportValues);
        assertEquals(1, passportValues.size());

        PassportValueEntity passportValue = passportValues.get(0);
        assertEquals(REFBOOK_NAME_KEY, passportValue.getAttribute().getCode());
        assertEquals(REFBOOK_NAME_VALUE, passportValue.getValue());
        assertEquals(entity,passportValue.getVersion());
    }

    @Test
    public void testCreateWithoutPassport() {

        RefBookEntity refBookEntity = createRefBookEntity();
        RefBookVersionEntity entity = strategy.create(refBookEntity, new Structure(), null);

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

    private List<PassportValueEntity> createPassportValues() {

        List<PassportValueEntity> list = new ArrayList<>(1);
        list.add(new PassportValueEntity(new PassportAttributeEntity(REFBOOK_NAME_KEY), REFBOOK_NAME_VALUE, null));

        return list;
    }
}