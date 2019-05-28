package ru.inovus.ms.rdm.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Conflict;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConflictServiceTest {

    private static final String REFERRER_REF_BOOK_CODE = "TEST_REFERRER_BOOK";
    private static final Integer REFERRER_VERSION_ID = -1;

    private static final String PUBLISHED_REF_BOOK_CODE = "TEST_PUBLISHED_BOOK";
    private static final Integer PUBLISHED_VERSION_ID = -2;
    private static final String PUBLISHED_PRIMARY_CODE = "code";

    @InjectMocks
    private ConflictServiceImpl conflictService;

    @Mock
    private VersionService versionService;
    @Mock
    private DraftService draftService;
    @Mock
    private DraftDataService draftDataService;

    @Mock
    private RefBookVersionRepository versionRepository;

    private RefBookVersion referrerVersion;
    private RefBookVersion publishedVersion;

    @Before
    public void setUp() {
        referrerVersion = new RefBookVersion();
        referrerVersion.setCode(REFERRER_REF_BOOK_CODE);
        referrerVersion.setId(REFERRER_VERSION_ID);
        referrerVersion.setStatus(RefBookVersionStatus.DRAFT);

        Structure referrerStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("str", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build("ref", "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference("ref", PUBLISHED_REF_BOOK_CODE, "${name}: ${amount}")
                )
            );
        referrerVersion.setStructure(referrerStructure);

        publishedVersion = new RefBookVersion();
        publishedVersion.setCode(PUBLISHED_REF_BOOK_CODE);
        publishedVersion.setId(PUBLISHED_VERSION_ID);
        publishedVersion.setStatus(RefBookVersionStatus.DRAFT);

        Structure publishedStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary(PUBLISHED_PRIMARY_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build("name", "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build("amount", "Количество", FieldType.INTEGER, "количество единиц")
                ),
                emptyList()
            );
        publishedVersion.setStructure(publishedStructure);
    }

    @Test
    public void calculateConflicts() {
    }

    @Test
    public void updateReferenceValues() {
        when(versionRepository.existsById(anyInt())).thenReturn(true);
        when(versionService.getById(eq(REFERRER_VERSION_ID))).thenReturn(referrerVersion);
        when(versionService.getById(eq(PUBLISHED_VERSION_ID))).thenReturn(publishedVersion);

        // NB: Add whens for versionService.search.

        // NB: Multiple primary keys are not supported yet.
        List<Conflict> conflicts = new ArrayList<>();

        List<FieldValue> updatedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(PUBLISHED_PRIMARY_CODE, "2")
                )
        );
        conflicts.add(new Conflict(ConflictType.UPDATED, updatedValues));

        List<FieldValue> deletedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(PUBLISHED_PRIMARY_CODE, "202")
                )
        );
        conflicts.add(new Conflict(ConflictType.DELETED, deletedValues));

        // NB: Use try-catch before implement getRefToRowValue and getRefFromRowValues.
        try {
            conflictService.updateReferenceValues(REFERRER_VERSION_ID, PUBLISHED_VERSION_ID, conflicts);
        }
        catch (RdmException e) {

        }
    }
}