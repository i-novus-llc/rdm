package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.strategy.DefaultBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.field;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.*;

public class DefaultCreateAttributeStrategyTest extends DefaultBaseStrategyTest {

    @InjectMocks
    private DefaultCreateAttributeStrategy strategy;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private StructureChangeValidator structureChangeValidator;

    @Test
    public void testCreateAttribute() {

        RefBookVersionEntity entity = createDraftEntity();
        fillOptLockValue(entity, DRAFT_OPT_LOCK_VALUE);

        final Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        CreateAttributeRequest request = new CreateAttributeRequest(DRAFT_OPT_LOCK_VALUE, newAttribute, null);

        Structure.Attribute attribute = strategy.create(entity, request);
        assertEquals(newAttribute, attribute); // Добавленный атрибут

        final Structure structure = entity.getStructure(); // Обычный атрибут:
        final String newAttributeCode = newAttribute.getCode();
        assertEquals(newAttribute, structure.getAttribute(newAttributeCode));
        assertNull(structure.getReference(newAttributeCode)); // без ссылки

        final String refBookCode = entity.getRefBook().getCode();
        verify(versionValidation).validateNewAttribute(
                eq(newAttribute), any(Structure.class), eq(refBookCode)
        );
        verify(versionValidation, never()).validateNewReference(
                any(Structure.Attribute.class), any(Structure.Reference.class), any(Structure.class), eq(refBookCode)
        );

        verify(draftDataService).addField(eq(entity.getStorageCode()), eq(field(newAttribute)));
    }

    @Test
    public void testCreateReference() {

        RefBookVersionEntity entity = createDraftEntity();

        final Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_REF_ATTRIBUTE);
        final Structure.Reference newReference = new Structure.Reference(CHANGE_REF_REFERENCE);
        CreateAttributeRequest request = new CreateAttributeRequest(DRAFT_OPT_LOCK_VALUE, newAttribute, newReference);

        Structure.Attribute attribute = strategy.create(entity, request);
        assertEquals(newAttribute, attribute); // Добавленный атрибут

        final Structure structure = entity.getStructure(); // Атрибут-ссылка:
        final String newAttributeCode = newAttribute.getCode();
        assertEquals(newAttribute, structure.getAttribute(newAttributeCode));
        assertEquals(newReference, structure.getReference(newAttributeCode));

        final String refBookCode = entity.getRefBook().getCode();
        verify(versionValidation).validateNewAttribute(
                eq(newAttribute), any(Structure.class), eq(refBookCode)
        );
        verify(versionValidation).validateNewReference(
                eq(newAttribute), eq(newReference), any(Structure.class), eq(refBookCode)
        );

        verify(draftDataService).addField(eq(entity.getStorageCode()), eq(field(newAttribute)));
    }
}