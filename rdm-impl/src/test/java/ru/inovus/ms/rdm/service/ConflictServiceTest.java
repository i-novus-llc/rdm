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
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookConflictRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private static final Integer REFERRER_DRAFT_ID = -5; //REFERRER_VERSION_ID;
    private static final String REFERRER_DRAFT_STORAGE_CODE = "TEST_REFERRER_STORAGE";
    private static final String REFERRER_PRIMARY_CODE = "str";

    private static final String REFERRER_PRIMARY_UPDATED_VALUE = "r2";
    private static final String REFERRER_PRIMARY_UNUPDATED_VALUE = "r3";
    private static final String REFERRER_PRIMARY_DELETED_VALUE = "r202";

    private static final Integer PUBLISHING_DRAFT_ID = -6; //PUBLISHING_VERSION_ID;
    private static final String PUBLISHING_DRAFT_STORAGE_CODE = "TEST_PUBLISHING_STORAGE";
    private static final String PUBLISHING_PRIMARY_CODE = "code";

    private static final String PUBLISHING_PRIMARY_UPDATED_VALUE = "2";
    private static final String PUBLISHING_PRIMARY_UNUPDATED_VALUE = "3";
    private static final String PUBLISHING_PRIMARY_DELETED_VALUE = "202";
    private static final String PUBLISHING_PRIMARY_UPDATED_DISPLAY = "Doubled_Two: 2222";

    @InjectMocks
    private ConflictServiceImpl conflictService;

    @Mock
    private VersionService versionService;
    @Mock
    private DraftService draftService;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookConflictRepository conflictRepository;

    private RefBookVersionEntity referrerEntity;
    private RefBookVersionEntity publishingEntity;

    @Before
    public void setUp() {
        referrerEntity = createReferrerEntity();
        publishingEntity = createPublishingEntity();

        doNothing().when(versionValidation).validateVersionExists(anyInt());

        when(versionRepository.getOne(eq(REFERRER_VERSION_ID))).thenReturn(referrerEntity);
        when(versionRepository.getOne(eq(PUBLISHING_VERSION_ID))).thenReturn(publishingEntity);
    }

    private RefBookVersionEntity createReferrerEntity() {
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(REFERRER_REF_BOOK_ID);
        refBookEntity.setCode(REFERRER_REF_BOOK_CODE);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(REFERRER_VERSION_ID);
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary(REFERRER_PRIMARY_CODE, "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_REFERENCE_ATTRIBUTE_CODE, "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference(REFERRER_REFERENCE_ATTRIBUTE_CODE, PUBLISHING_REF_BOOK_CODE, REFERRER_REFERENCE_DISPLAY_EXPRESSION)
                )
            );
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    private RefBookVersionEntity createPublishingEntity() {
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(PUBLISHING_REF_BOOK_ID);
        refBookEntity.setCode(PUBLISHING_REF_BOOK_CODE);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(PUBLISHING_VERSION_ID);
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary(PUBLISHING_PRIMARY_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build("name", "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build("amount", "Количество", FieldType.INTEGER, "количество единиц")
                ),
                emptyList()
        );
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    @Test
    public void testCalculateConflicts() {
    }

    @Test
    public void testUpdateReferenceValues() {

        referrerEntity.setStorageCode(REFERRER_DRAFT_STORAGE_CODE);
        publishingEntity.setStorageCode(PUBLISHING_DRAFT_STORAGE_CODE);

        List<Conflict> conflicts = createUpdateReferenceConflicts();

        SearchDataCriteria referrerUpdatedCriteria = createUpdateReferenceReferrerCriteria(REFERRER_PRIMARY_UPDATED_VALUE);
        PageImpl<RefBookRowValue> referrerUpdatedRows = createUpdateReferenceReferrerRows(PUBLISHING_PRIMARY_UPDATED_VALUE, "Two: 22");
        when(versionService.search(eq(REFERRER_VERSION_ID), eq(referrerUpdatedCriteria))).thenReturn(referrerUpdatedRows);

        SearchDataCriteria referrerUnupdatedCriteria = createUpdateReferenceReferrerCriteria(REFERRER_PRIMARY_UNUPDATED_VALUE);
        PageImpl<RefBookRowValue> referrerUnupdatedRows = createUpdateReferenceReferrerRows(PUBLISHING_PRIMARY_UNUPDATED_VALUE, "Three: 33");
        when(versionService.search(eq(REFERRER_VERSION_ID), eq(referrerUnupdatedCriteria))).thenReturn(referrerUnupdatedRows);

        SearchDataCriteria publishingUpdatedCriteria = createUpdateReferencePublishingCriteria(PUBLISHING_PRIMARY_UPDATED_VALUE);
        PageImpl<RefBookRowValue> publishingUpdatedRows = createUpdateReferencePublishingRows("Doubled_Two", 2222);
        when(versionService.search(eq(PUBLISHING_VERSION_ID), eq(publishingUpdatedCriteria))).thenReturn(publishingUpdatedRows);

        SearchDataCriteria publishingUnupdatedCriteria = createUpdateReferencePublishingCriteria(PUBLISHING_PRIMARY_UNUPDATED_VALUE);
        PageImpl<RefBookRowValue> publishingUnupdatedRows = createUpdateReferencePublishingRows("Three", 33);
        when(versionService.search(eq(PUBLISHING_VERSION_ID), eq(publishingUnupdatedCriteria))).thenReturn(publishingUnupdatedRows);

        ArgumentCaptor<RefBookRowValue> rowValueCaptor = ArgumentCaptor.forClass(RefBookRowValue.class);

        conflictService.updateReferenceValues(REFERRER_VERSION_ID, PUBLISHING_VERSION_ID, conflicts);

        verify(draftDataService, times(1)).updateRow(eq(REFERRER_DRAFT_STORAGE_CODE), rowValueCaptor.capture());

        PageImpl<RefBookRowValue> updatedRows = createUpdateReferenceReferrerRows(PUBLISHING_PRIMARY_UPDATED_VALUE, PUBLISHING_PRIMARY_UPDATED_DISPLAY);
        RefBookRowValue updatedRow = updatedRows.get().findFirst().orElse(null);
        Assert.assertNotNull(updatedRow);

        Reference expectedReference = new Reference(
                PUBLISHING_DRAFT_STORAGE_CODE,
                null,
                PUBLISHING_PRIMARY_CODE, // referenceAttribute
                new DisplayExpression(REFERRER_REFERENCE_DISPLAY_EXPRESSION),
                PUBLISHING_PRIMARY_UPDATED_VALUE,
                PUBLISHING_PRIMARY_UPDATED_DISPLAY);
        ReferenceFieldValue expectedFieldValue = new ReferenceFieldValue(REFERRER_REFERENCE_ATTRIBUTE_CODE, expectedReference);
        LongRowValue expectedRowValue = new LongRowValue(updatedRow.getSystemId(), singletonList(expectedFieldValue));
        Assert.assertEquals(new RefBookRowValue(expectedRowValue, REFERRER_VERSION_ID), rowValueCaptor.getValue());
    }

    private List<Conflict> createUpdateReferenceConflicts() {
        // NB: Multiple primary keys are not supported yet.
        List<Conflict> conflicts = new ArrayList<>();

        List<FieldValue> updatedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(REFERRER_PRIMARY_CODE, REFERRER_PRIMARY_UPDATED_VALUE)
                )
        );
        conflicts.add(new Conflict(REFERRER_REFERENCE_ATTRIBUTE_CODE, ConflictType.UPDATED, updatedValues));

        List<FieldValue> unupdatedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(REFERRER_PRIMARY_CODE, REFERRER_PRIMARY_UNUPDATED_VALUE)
                )
        );
        conflicts.add(new Conflict(REFERRER_REFERENCE_ATTRIBUTE_CODE, ConflictType.UPDATED, unupdatedValues));

        List<FieldValue> deletedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(REFERRER_PRIMARY_CODE, REFERRER_PRIMARY_DELETED_VALUE)
                )
        );
        conflicts.add(new Conflict(REFERRER_REFERENCE_ATTRIBUTE_CODE, ConflictType.DELETED, deletedValues));

        return conflicts;
    }

    public static SearchDataCriteria createUpdateReferenceReferrerCriteria(String referenceValue) {

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        AttributeFilter filter = new AttributeFilter(REFERRER_PRIMARY_CODE, referenceValue, FieldType.STRING, SearchTypeEnum.EXACT);
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
                                new Reference(PUBLISHING_DRAFT_STORAGE_CODE,
                                        null,
                                        PUBLISHING_PRIMARY_CODE,
                                        displayExpression,
                                        referenceValue,
                                        displayValue))
                ), REFERRER_VERSION_ID)
        ), PageRequest.of(0, 10), 1);
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

}