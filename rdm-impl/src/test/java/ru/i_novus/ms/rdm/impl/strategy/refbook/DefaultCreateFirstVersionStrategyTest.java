package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCreateFirstVersionStrategyTest {

    @InjectMocks
    private DefaultCreateFirstVersionStrategy strategy;

    @Test
    public void testCreate() {

        final String refBookCode = "test_code";
        final String refBookName = "name";

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode(refBookCode);

        Map<String, String> passport = new HashMap<>(1);
        passport.put(refBookName, "Reference book");

        RefBookCreateRequest request = new RefBookCreateRequest(refBookCode, RefBookType.UNVERSIONED, "category", passport);
        RefBookVersionEntity entity = strategy.create(refBookEntity, request);

        assertEquals(refBookEntity, entity.getRefBook());
        assertEquals(RefBookVersionStatus.DRAFT, entity.getStatus());
        assertTrue(StringUtils.isEmpty(entity.getVersion()));
        assertEquals(entity.getStructure(), new Structure());

        Map<String, String> actualPassport = entity.toPassport();
        assertNotNull(actualPassport);
        assertEquals(1, actualPassport.size());
        assertEquals(passport.get(refBookName), actualPassport.get(refBookName));
    }
}