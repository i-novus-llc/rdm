package ru.inovus.ms.rdm.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConflictServiceTest {

    private static final Integer REFERRER_REF_BOOK_ID = -1;
    private static final String REFERRER_REF_BOOK_CODE = "TEST_REFERRER_BOOK";
    private static final Integer REFERRER_VERSION_ID = -3;
    private static final String REFERRER_REFERENCE_ATTRIBUTE_CODE = "ref";
    private static final String REFERRER_REFERENCE_DISPLAY_EXPRESSION = "${name}: ${amount}";

    private static final Integer PUBLISHING_REF_BOOK_ID = -2;
    private static final String PUBLISHING_REF_BOOK_CODE = "TEST_PUBLISHED_BOOK";
    private static final Integer PUBLISHING_VERSION_ID = -4;
    private static final String PUBLISHING_VERSION_STORAGE_CODE = "TEST_PUBLISHING_STORAGE";
    private static final String PUBLISHING_PRIMARY_CODE = "code";

    private static final Integer REFERRER_DRAFT_ID = -5; //REFERRER_VERSION_ID;
    private static final String REFERRER_DRAFT_STORAGE_CODE = "TEST_DRAFT_STORAGE";

    private static final String PUBLISHING_PRIMARY_UPDATED_VALUE = "2";
    private static final String PUBLISHING_PRIMARY_DELETED_VALUE = "202";

    @InjectMocks
    private ConflictServiceImpl conflictService;

    @Mock
    private RefBookService refBookService;
    @Mock
    private VersionService versionService;
    @Mock
    private DraftService draftService;
    @Mock
    private DraftDataService draftDataService;

    @Mock
    private RefBookVersionRepository versionRepository;

    private RefBookVersion referrerVersion;
    private RefBookVersion publishingVersion;
    private Draft referrerDraft;
    private RefBookVersion referrerDraftVersion;

    @Before
    public void setUp() {
        createReferrerVersion();
        createPublishingVersion();
        createReferrerDraft();
        createReferrerDraftVersion();

        when(versionRepository.existsById(anyInt())).thenReturn(true);
        when(versionService.getById(eq(REFERRER_VERSION_ID))).thenReturn(referrerVersion);
        when(versionService.getById(eq(PUBLISHING_VERSION_ID))).thenReturn(publishingVersion);
        when(refBookService.getCode(eq(PUBLISHING_REF_BOOK_ID))).thenReturn(PUBLISHING_REF_BOOK_CODE);
    }

    private void createReferrerVersion() {
        referrerVersion = new RefBookVersion();
        referrerVersion.setCode(REFERRER_REF_BOOK_CODE);
        referrerVersion.setId(REFERRER_VERSION_ID);
        referrerVersion.setRefBookId(REFERRER_REF_BOOK_ID);
        referrerVersion.setStatus(RefBookVersionStatus.DRAFT);

        Structure referrerStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("str", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_REFERENCE_ATTRIBUTE_CODE, "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference(REFERRER_REFERENCE_ATTRIBUTE_CODE, PUBLISHING_REF_BOOK_CODE, REFERRER_REFERENCE_DISPLAY_EXPRESSION)
                )
            );
        referrerVersion.setStructure(referrerStructure);
    }

    private void createPublishingVersion() {
        publishingVersion = new RefBookVersion();
        publishingVersion.setCode(PUBLISHING_REF_BOOK_CODE);
        publishingVersion.setId(PUBLISHING_VERSION_ID);
        publishingVersion.setRefBookId(PUBLISHING_REF_BOOK_ID);
        publishingVersion.setStatus(RefBookVersionStatus.DRAFT);

        Structure publishingStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary(PUBLISHING_PRIMARY_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build("name", "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build("amount", "Количество", FieldType.INTEGER, "количество единиц")
                ),
                emptyList()
        );
        publishingVersion.setStructure(publishingStructure);
    }

    private void createReferrerDraft() {
        referrerDraft = new Draft();
        referrerDraft.setId(REFERRER_DRAFT_ID);
        referrerDraft.setStorageCode(REFERRER_DRAFT_STORAGE_CODE);
    }

    private void createReferrerDraftVersion() {
        referrerDraftVersion = new RefBookVersion();
        referrerDraftVersion.setCode(REFERRER_REF_BOOK_CODE);
        referrerDraftVersion.setId(REFERRER_DRAFT_ID);
        referrerDraftVersion.setRefBookId(REFERRER_REF_BOOK_ID);
        referrerDraftVersion.setStatus(RefBookVersionStatus.DRAFT);

        Structure referrerDraftStructure = new Structure(referrerVersion.getStructure());
        referrerDraftVersion.setStructure(referrerDraftStructure);
    }

    @Test
    public void calculateConflicts() {
    }

    @Test
    public void updateReferenceValues() {

        when(draftService.getDraft(eq(REFERRER_VERSION_ID))).thenReturn(referrerDraft);
        when(versionService.getById(eq(REFERRER_DRAFT_ID))).thenReturn(referrerDraftVersion);

        List<Conflict> conflicts = createUpdateReferenceConflicts();

        PageImpl<RefBookRowValue> publishingRows = createUpdateReferencePublishingRows();
        when(versionService.search(eq(PUBLISHING_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(publishingRows);

        PageImpl<RefBookRowValue> referrerRows = createUpdateReferenceReferrerRows();
        when(versionService.search(eq(REFERRER_DRAFT_ID), any(SearchDataCriteria.class))).thenReturn(referrerRows);

        conflictService.updateReferenceValues(REFERRER_VERSION_ID, PUBLISHING_VERSION_ID, conflicts);
    }

    private List<Conflict> createUpdateReferenceConflicts() {
        // NB: Multiple primary keys are not supported yet.
        List<Conflict> conflicts = new ArrayList<>();

        List<FieldValue> updatedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(PUBLISHING_PRIMARY_CODE, PUBLISHING_PRIMARY_UPDATED_VALUE)
                )
        );
        conflicts.add(new Conflict(ConflictType.UPDATED, updatedValues));

        List<FieldValue> deletedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(PUBLISHING_PRIMARY_CODE, PUBLISHING_PRIMARY_DELETED_VALUE)
                )
        );
        conflicts.add(new Conflict(ConflictType.DELETED, deletedValues));

        return conflicts;
    }

    public static PageImpl<RefBookRowValue> createUpdateReferencePublishingRows() {
        return new PageImpl<>(singletonList(
                new RefBookRowValue(new LongRowValue(
                        new StringFieldValue(PUBLISHING_PRIMARY_CODE, PUBLISHING_PRIMARY_UPDATED_VALUE),
                        new StringFieldValue("name", "Doubled_Two"),
                        new IntegerFieldValue("amount", BigInteger.valueOf(2222))
                ), PUBLISHING_VERSION_ID)
        ), PageRequest.of(0, 10), 1);
    }

    public static PageImpl<RefBookRowValue> createUpdateReferenceReferrerRows() {
        return new PageImpl<>(singletonList(
                new RefBookRowValue(new LongRowValue(
                        new StringFieldValue("str", "str-referrer"),
                        new ReferenceFieldValue(REFERRER_REFERENCE_ATTRIBUTE_CODE,
                                new Reference(PUBLISHING_VERSION_STORAGE_CODE,
                                        TimeUtils.now(),
                                        PUBLISHING_PRIMARY_CODE,
                                        REFERRER_REFERENCE_DISPLAY_EXPRESSION,
                                        PUBLISHING_PRIMARY_UPDATED_VALUE,
                                        "Two: 22"))
                ), REFERRER_DRAFT_ID)
        ), PageRequest.of(0, 10), 1);
    }

}