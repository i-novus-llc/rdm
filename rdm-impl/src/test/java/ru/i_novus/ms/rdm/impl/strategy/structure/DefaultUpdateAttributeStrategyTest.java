package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.DefaultBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.field;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.*;

public class DefaultUpdateAttributeStrategyTest extends DefaultBaseStrategyTest {

    @InjectMocks
    private DefaultUpdateAttributeStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private StructureChangeValidator structureChangeValidator;

    @Test
    public void testUpdateAttribute() {

        RefBookVersionEntity entity = createDraftEntity();
        fillOptLockValue(entity, DRAFT_OPT_LOCK_VALUE);

        final Structure.Attribute oldAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        entity.getStructure().add(oldAttribute, null);

        final Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        newAttribute.setName(newAttribute.getName() + "_update");

        UpdateAttributeRequest request = new UpdateAttributeRequest(DRAFT_OPT_LOCK_VALUE, newAttribute, null);

        Structure.Attribute attribute = strategy.update(entity, request);
        assertEquals(newAttribute, attribute); // Изменённый атрибут

        final Structure structure = entity.getStructure(); // Обычный атрибут:
        final String newAttributeCode = newAttribute.getCode();
        assertEquals(newAttribute, structure.getAttribute(newAttributeCode));
        assertNull(structure.getReference(newAttributeCode)); // без ссылки

        verify(draftDataService).updateField(eq(entity.getStorageCode()), eq(field(newAttribute)));
    }

    @Test
    public void testUpdateReference() {

        RefBookVersionEntity entity = createDraftEntity();

        Structure.Attribute oldAttribute = new Structure.Attribute(CHANGE_REF_ATTRIBUTE);
        Structure.Reference oldReference = new Structure.Reference(CHANGE_REF_REFERENCE);
        entity.getStructure().add(oldAttribute, oldReference);

        Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_REF_ATTRIBUTE);
        newAttribute.setName(newAttribute.getName() + "_update");
        Structure.Reference newReference = new Structure.Reference(CHANGE_REF_REFERENCE);
        newReference.setDisplayExpression(newReference.getDisplayExpression() + "_update");

        RefBookVersionEntity referredEntity = createReferredVersionEntity(newReference);

        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                newReference.getReferenceCode(), RefBookVersionStatus.PUBLISHED
        ))
                .thenReturn(referredEntity);

        UpdateAttributeRequest request = new UpdateAttributeRequest(DRAFT_OPT_LOCK_VALUE, newAttribute, newReference);
        Structure.Attribute attribute = strategy.update(entity, request);
        assertEquals(newAttribute, attribute); // Изменённый атрибут

        final Structure structure = entity.getStructure(); // Атрибут-ссылка:
        final String newAttributeCode = newAttribute.getCode();
        assertEquals(newAttribute, structure.getAttribute(newAttributeCode));
        assertEquals(newReference, structure.getReference(newAttributeCode));

        verify(draftDataService).updateField(eq(entity.getStorageCode()), eq(field(newAttribute)));
    }
}