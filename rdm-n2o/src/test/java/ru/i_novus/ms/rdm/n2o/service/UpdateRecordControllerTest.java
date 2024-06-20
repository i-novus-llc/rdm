package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.rest.client.impl.ConflictServiceRestClient;
import ru.i_novus.ms.rdm.rest.client.impl.VersionRestServiceRestClient;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateRecordControllerTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final int TEST_REFERRED_VERSION_ID = -20;

    private static final long TEST_SYSTEM_ID = 51;

    @InjectMocks
    private UpdateRecordController controller;

    @Mock
    private VersionRestServiceRestClient versionService;

    @Mock
    private ConflictServiceRestClient conflictService;

    @Mock
    private Messages messages;

    @Test
    public void getDataConflicts() {

        final Structure structure = createStructure();
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(structure);

        final RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(TEST_REFBOOK_VERSION_ID);
        criteria.setIsLastPublishedVersion(true);
        criteria.setRefFieldCodes(List.of(REFERENCE_ATTRIBUTE_CODE));
        criteria.setRefRecordId(TEST_SYSTEM_ID);

        final List<RefBookConflict> conflicts = createDataConflicts();
        when(conflictService.search(any(RefBookConflictCriteria.class)))
                .thenReturn(new PageImpl<>(conflicts, criteria, conflicts.size()));

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(messages.getMessage(eq("conflict.text"), eq(REFERENCE_ATTRIBUTE_CODE.toLowerCase()), any(String.class))).thenAnswer(invocation -> invocation.getArguments()[2]);

        final String actual = controller.getDataConflicts(TEST_REFBOOK_VERSION_ID, TEST_SYSTEM_ID);
        assertNotNull(actual);

        final String[] lines = actual.split("\n");
        assertNotNull(lines);

        final long expectedCount = conflicts.stream().filter(conflict -> conflict.getRefRecordId() > 0).count();
        assertEquals(expectedCount, lines.length);

        for (String line : lines) {
            assertTrue(line.contains("conflict.text"));
        }
    }

    @Test
    public void getDataConflictsWhenEmpty() {

        final Structure structure = new Structure();
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(structure);

        final String actual = controller.getDataConflicts(TEST_REFBOOK_VERSION_ID, TEST_SYSTEM_ID);
        assertNull(actual);
    }

    @Test
    public void getDataConflictsWhenFailed() {

        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenThrow(new IllegalArgumentException());

        final String actual = controller.getDataConflicts(TEST_REFBOOK_VERSION_ID, TEST_SYSTEM_ID);
        assertNull(actual);
    }

    @Test
    public void getDataConflictsWithoutReferences() {

        final Structure structure = new Structure();
        structure.add(Structure.Attribute.buildPrimary("single", "одиночный", FieldType.INTEGER, null), null);
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(structure);

        final String actual = controller.getDataConflicts(TEST_REFBOOK_VERSION_ID, TEST_SYSTEM_ID);
        assertNull(actual);
    }

    @Test
    public void getDataConflictsWithoutConflicts() {

        final Structure structure = createStructure();
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(structure);

        final String actual = controller.getDataConflicts(TEST_REFBOOK_VERSION_ID, TEST_SYSTEM_ID);
        assertNull(actual);
    }

    /** Создание структуры с глубоким копированием атрибутов и ссылок. */
    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }

    private List<RefBookConflict> createDataConflicts() {

        final RefBookConflict updatedConflict = new RefBookConflict(
                TEST_REFBOOK_VERSION_ID, TEST_REFERRED_VERSION_ID, 1L,
                REFERENCE_ATTRIBUTE_CODE, ConflictType.UPDATED, LocalDateTime.now()
        );

        final RefBookConflict deletedConflict = new RefBookConflict(
                TEST_REFBOOK_VERSION_ID, TEST_REFERRED_VERSION_ID, 2L,
                REFERENCE_ATTRIBUTE_CODE, ConflictType.DELETED, LocalDateTime.now()
        );

        final RefBookConflict alteredConflict = new RefBookConflict(
                TEST_REFBOOK_VERSION_ID, TEST_REFERRED_VERSION_ID, -3L,
                SELF_REFER_ATTRIBUTE_CODE, ConflictType.ALTERED, LocalDateTime.now()
        );

        final RefBookConflict nullConflict = new RefBookConflict(
                TEST_REFBOOK_VERSION_ID, TEST_REFERRED_VERSION_ID, -4L,
                SELF_REFER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED, LocalDateTime.now()
        );

        final RefBookConflict unknownConflict = new RefBookConflict(
                TEST_REFBOOK_VERSION_ID, TEST_REFERRED_VERSION_ID, -5L,
                UNKNOWN_ATTRIBUTE_CODE, ConflictType.DELETED, LocalDateTime.now()
        );

        return List.of(updatedConflict, deletedConflict, alteredConflict, nullConflict, unknownConflict);
    }
}