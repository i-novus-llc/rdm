package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.DefaultBaseStrategyTest;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.*;

public class DefaultDeleteAttributeStrategyTest extends DefaultBaseStrategyTest {

    @InjectMocks
    private DefaultDeleteAttributeStrategy strategy;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private VersionValidation versionValidation;

    @Test
    public void testDeleteAttribute() {

        RefBookVersionEntity entity = createDraftEntity();
        fillOptLockValue(entity, DRAFT_OPT_LOCK_VALUE);

        final Structure.Attribute oldAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        entity.getStructure().add(oldAttribute, null);

        final String oldAttributeCode = oldAttribute.getCode();
        DeleteAttributeRequest request = new DeleteAttributeRequest(DRAFT_OPT_LOCK_VALUE, oldAttributeCode);

        Structure.Attribute attribute = strategy.delete(entity, request);
        assertEquals(oldAttribute, attribute); // Удалённый атрибут

        final Structure structure = entity.getStructure(); // Обычный атрибут:
        assertNull(structure.getAttribute(oldAttributeCode));
        assertNull(structure.getReference(oldAttributeCode)); // без ссылки

        verify(draftDataService).deleteField(eq(entity.getStorageCode()), eq(oldAttributeCode));
    }

    @Test
    public void testDeleteReference() {

        RefBookVersionEntity entity = createDraftEntity();
        fillOptLockValue(entity, DRAFT_OPT_LOCK_VALUE);

        Structure.Attribute oldAttribute = new Structure.Attribute(CHANGE_REF_ATTRIBUTE);
        Structure.Reference oldReference = new Structure.Reference(CHANGE_REF_REFERENCE);
        entity.getStructure().add(oldAttribute, oldReference);

        final String oldAttributeCode = oldAttribute.getCode();
        DeleteAttributeRequest request = new DeleteAttributeRequest(DRAFT_OPT_LOCK_VALUE, oldAttributeCode);
        Structure.Attribute attribute = strategy.delete(entity, request);
        assertEquals(oldAttribute, attribute); // Удалённый атрибут

        final Structure structure = entity.getStructure(); // Атрибут-ссылка:
        assertNull(structure.getAttribute(oldAttributeCode));
        assertNull(structure.getReference(oldAttributeCode));

        verify(draftDataService).deleteField(eq(entity.getStorageCode()), eq(oldAttributeCode));
    }
}