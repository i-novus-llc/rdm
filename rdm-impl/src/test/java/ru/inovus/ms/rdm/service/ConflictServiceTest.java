package ru.inovus.ms.rdm.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
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
import static java.util.Collections.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    private static final String PUBLISHING_PRIMARY_UNUPDATED_VALUE = "3";
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

        when(versionRepository.existsById(anyInt())).thenReturn(true);
        when(versionService.getById(eq(REFERRER_VERSION_ID))).thenReturn(referrerVersion);
        when(versionService.getById(eq(PUBLISHING_VERSION_ID))).thenReturn(publishingVersion);
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
    public void testCalculateConflicts() {
    }

    @Test
    public void testUpdateReferenceValues() {

        createReferrerDraft();
        createReferrerDraftVersion();

        when(draftService.getDraft(eq(REFERRER_VERSION_ID))).thenReturn(referrerDraft);
        when(versionService.getById(eq(REFERRER_DRAFT_ID))).thenReturn(referrerDraftVersion);

        List<Conflict> conflicts = createUpdateReferenceConflicts();

        SearchDataCriteria publishingUpdatedCriteria = createUpdateReferencePublishingCriteria(PUBLISHING_PRIMARY_UPDATED_VALUE);
        PageImpl<RefBookRowValue> publishingUpdatedRows = createUpdateReferencePublishingRows("Doubled_Two", 2222);
        when(versionService.search(eq(PUBLISHING_VERSION_ID), eq(publishingUpdatedCriteria))).thenReturn(publishingUpdatedRows);

        SearchDataCriteria publishingUnupdatedCriteria = createUpdateReferencePublishingCriteria(PUBLISHING_PRIMARY_UNUPDATED_VALUE);
        PageImpl<RefBookRowValue> publishingUnupdatedRows = createUpdateReferencePublishingRows("Three", 33);
        when(versionService.search(eq(PUBLISHING_VERSION_ID), eq(publishingUnupdatedCriteria))).thenReturn(publishingUnupdatedRows);

        SearchDataCriteria referrerUpdatedCriteria = createUpdateReferenceReferrerCriteria(PUBLISHING_PRIMARY_UPDATED_VALUE);
        PageImpl<RefBookRowValue> referrerUpdatedRows = createUpdateReferenceReferrerRows(PUBLISHING_PRIMARY_UPDATED_VALUE, "Two: 22");
        when(versionService.search(eq(REFERRER_DRAFT_ID), eq(referrerUpdatedCriteria))).thenReturn(referrerUpdatedRows);

        SearchDataCriteria referrerUnupdatedCriteria = createUpdateReferenceReferrerCriteria(PUBLISHING_PRIMARY_UNUPDATED_VALUE);
        PageImpl<RefBookRowValue> referrerUnupdatedRows = createUpdateReferenceReferrerRows(PUBLISHING_PRIMARY_UNUPDATED_VALUE, "Three: 33");
        when(versionService.search(eq(REFERRER_DRAFT_ID), eq(referrerUnupdatedCriteria))).thenReturn(referrerUnupdatedRows);

        ArgumentCaptor<RefBookRowValue> rowValueCaptor = ArgumentCaptor.forClass(RefBookRowValue.class);

        conflictService.updateReferenceValues(REFERRER_VERSION_ID, PUBLISHING_VERSION_ID, conflicts);

        verify(draftDataService, times(1)).updateRow(eq(REFERRER_DRAFT_STORAGE_CODE), rowValueCaptor.capture());

        PageImpl<RefBookRowValue> updatedRows = createUpdateReferenceReferrerRows(PUBLISHING_PRIMARY_UPDATED_VALUE, "Doubled_Two: 2222");
        Assert.assertEquals(updatedRows.get().findFirst().orElse(null), rowValueCaptor.getValue());
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

        List<FieldValue> unupdatedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(PUBLISHING_PRIMARY_CODE, PUBLISHING_PRIMARY_UNUPDATED_VALUE)
                )
        );
        conflicts.add(new Conflict(ConflictType.UPDATED, unupdatedValues));

        List<FieldValue> deletedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(PUBLISHING_PRIMARY_CODE, PUBLISHING_PRIMARY_DELETED_VALUE)
                )
        );
        conflicts.add(new Conflict(ConflictType.DELETED, deletedValues));

        return conflicts;
    }

    public static SearchDataCriteria createUpdateReferencePublishingCriteria(String codeValue) {

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        AttributeFilter filter = new AttributeFilter(PUBLISHING_PRIMARY_CODE, codeValue, FieldType.STRING, SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        return criteria;
    }

    public static PageImpl<RefBookRowValue> createUpdateReferencePublishingRows(String nameValue, Integer amountValue) {
        return new PageImpl<>(singletonList(
                new RefBookRowValue(new LongRowValue(
                        new StringFieldValue(PUBLISHING_PRIMARY_CODE, PUBLISHING_PRIMARY_UPDATED_VALUE),
                        new StringFieldValue("name", nameValue),
                        new IntegerFieldValue("amount", BigInteger.valueOf(amountValue))
                ), PUBLISHING_VERSION_ID)
        ), PageRequest.of(0, 10), 1);
    }

    public static SearchDataCriteria createUpdateReferenceReferrerCriteria(String referenceValue) {

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        AttributeFilter filter = new AttributeFilter(REFERRER_REFERENCE_ATTRIBUTE_CODE, referenceValue, FieldType.STRING, SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        return criteria;
    }

    public static PageImpl<RefBookRowValue> createUpdateReferenceReferrerRows(String referenceValue, String displayValue) {
        DisplayExpression displayExpression = new DisplayExpression(REFERRER_REFERENCE_DISPLAY_EXPRESSION);

        return new PageImpl<>(singletonList(
                new RefBookRowValue(new LongRowValue(
                        new StringFieldValue("str", "str-referrer"),
                        new ReferenceFieldValue(REFERRER_REFERENCE_ATTRIBUTE_CODE,
                                new Reference(PUBLISHING_VERSION_STORAGE_CODE,
                                        null,
                                        PUBLISHING_PRIMARY_CODE,
                                        displayExpression,
                                        referenceValue,
                                        displayValue))
                ), REFERRER_DRAFT_ID)
        ), PageRequest.of(0, 10), 1);
    }

}