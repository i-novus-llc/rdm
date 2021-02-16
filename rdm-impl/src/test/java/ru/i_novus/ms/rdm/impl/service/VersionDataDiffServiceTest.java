package ru.i_novus.ms.rdm.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.*;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.RefBookVersionDiffEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffResult;
import ru.i_novus.ms.rdm.impl.provider.VdsMapperConfigurer;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.RefBookVersionDiffRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.VersionDataDiffRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.VersionDataDiffResultRepository;
import ru.i_novus.ms.rdm.test.BaseTest;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.BooleanField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.util.CollectionUtils.isEmpty;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"rawtypes","java:S5778"})
public class VersionDataDiffServiceTest extends BaseTest {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final VdsMapperConfigurer VDS_MAPPER_CONFIGURER = new VdsMapperConfigurer();

    private static final String TEST_REFBOOK_CODE = "test_code";
    private static final Integer OLD_VERSION_ID = 1;
    private static final Integer MID_VERSION_ID = 2;
    private static final Integer NEW_VERSION_ID = 11;
    private static final Integer VERSION_DIFF_ID = 3;

    private static final String VERSION_ATTRIBUTE_ID = "id";
    private static final String VERSION_ATTRIBUTE_CODE = "code";
    private static final String VERSION_ATTRIBUTE_NAME = "name";
    private static final String VERSION_ATTRIBUTE_AMOUNT = "amount";
    private static final String VERSION_ATTRIBUTE_BOOL = "bool";
    private static final String VERSION_ATTRIBUTE_UPDATED = "updated";
    private static final String VERSION_ATTRIBUTE_INSERTED = "inserted";
    private static final String VERSION_ATTRIBUTE_DELETED = "deleted";

    private static final String ROW_INS_CODE = "code=789";
    private static final String ROW_INS_FIRST = "{\"status\": \"INSERTED\", \"values\": [" +
            "  {\"field\": {\"id\": \"IntegerField\", \"name\": \"code\", \"type\": \"bigint\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"INSERTED\", \"newValue\": 789}," +
            "  {\"field\": {\"id\": \"StringField\", \"name\": \"name\", \"type\": \"character varying\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"INSERTED\"}" +
            "]}";

    private static final String ROW_UPD_UPD_CODE = "code=7890";
    private static final String ROW_UPD_UPD_FIRST = "{\"status\": \"UPDATED\", \"values\": [" +
            "  {\"field\": {\"id\": \"IntegerField\", \"name\": \"code\", \"type\": \"bigint\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"newValue\": 7890}," +
            "  {\"field\": {\"id\": \"StringField\", \"name\": \"name\", \"type\": \"character varying\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"UPDATED\", \"oldValue\": \"TBRO\"}" +
            "]}";
    private static final String ROW_UPD_UPD_LAST = "{\"status\": \"UPDATED\", \"values\": [" +
            "  {\"field\": {\"id\": \"IntegerField\", \"name\": \"code\", \"type\": \"bigint\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"newValue\": 7890}," +
            "  {\"field\": {\"id\": \"StringField\", \"name\": \"name\", \"type\": \"character varying\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"UPDATED\", \"newValue\": \"T-B-R-O\"}" +
            "]}";

    private static final String ROW_INS_DEL_CODE = "code=1234";
    private static final String ROW_INS_DEL_FIRST = "{\"status\": \"INSERTED\", \"values\": [" +
            "  {\"field\": {\"id\": \"IntegerField\", \"name\": \"code\", \"type\": \"bigint\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"INSERTED\", \"newValue\": 1234}, " +
            "  {\"field\": {\"id\": \"StringField\", \"name\": \"name\", \"type\": \"character varying\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"INSERTED\"}" +
            "]}";
    private static final String ROW_INS_DEL_LAST = "{\"status\": \"DELETED\", \"values\": [" +
            "  {\"field\": {\"id\": \"IntegerField\", \"name\": \"code\", \"type\": \"bigint\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"DELETED\", \"oldValue\": 1234}," +
            "  {\"field\": {\"id\": \"StringField\", \"name\": \"name\", \"type\": \"character varying\"," +
            "       \"unique\": false, \"required\": false, \"searchEnabled\": false}," +
            "   \"status\": \"DELETED\", \"oldValue\": \"IZEY\"}" +
            "]}";

    @InjectMocks
    private VersionDataDiffServiceImpl service;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookVersionDiffRepository versionDiffRepository;
    @Mock
    private VersionDataDiffRepository dataDiffRepository;
    @Mock
    private VersionDataDiffResultRepository dataDiffResultRepository;

    @Mock
    private CompareService compareService;

    @Mock
    private VersionValidation versionValidation;

    @Before
    public void setUp() throws NoSuchFieldException {

        JsonUtil.jsonMapper = JSON_MAPPER;
        VDS_MAPPER_CONFIGURER.configure(JSON_MAPPER);
    }

    @Test
    public void testSerializationForField() {

        IntegerField field = new IntegerField("id");
        String jsonValue = JsonUtil.toJsonString(field);
        Field restored = JsonUtil.fromJsonString(jsonValue, Field.class);
        assertEquals(field.getClass(), restored.getClass());
        assertEquals(field, restored);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSerializationForDiffFieldValue() {

        IntegerField field = new IntegerField("id");
        DiffFieldValue diffFieldValue = new DiffFieldValue(field, null, BigInteger.valueOf(1L), null);
        String jsonValue = JsonUtil.toJsonString(diffFieldValue);
        DiffFieldValue restoredValue = JsonUtil.fromJsonString(jsonValue, DiffFieldValue.class);
        assertEquals(diffFieldValue.getField().getClass(), restoredValue.getField().getClass());
        assertEquals(diffFieldValue, restoredValue);
    }

    /** Поиск в случае без фильтрации. */
    @Test
    public void testSearch() {

        // getVersions:
        RefBookVersionEntity oldVersion = createVersionEntity(OLD_VERSION_ID);
        RefBookVersionEntity newVersion = createVersionEntity(NEW_VERSION_ID);
        when(versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                eq(List.of(OLD_VERSION_ID, NEW_VERSION_ID)), eq(RefBookVersionStatus.PUBLISHED)
        )).thenReturn(List.of(newVersion, oldVersion));

        // getVersionIds:
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode(TEST_REFBOOK_CODE);
        newVersion.setRefBook(refBookEntity);

        RefBookVersionEntity startVersion = createVersionEntity(NEW_VERSION_ID + 1);
        RefBookVersionEntity midVersion = createVersionEntity(MID_VERSION_ID);
        RefBookVersionEntity endVersion = createVersionEntity(OLD_VERSION_ID - 1);
        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(List.of(startVersion, newVersion, midVersion, oldVersion, endVersion));
        String versionIds = NEW_VERSION_ID + "," + MID_VERSION_ID + "," + OLD_VERSION_ID;

        // searchVersionDiffIds:
        String versionDiffIds = "{" + OLD_VERSION_ID * 10 + "," + MID_VERSION_ID * 10 + "," + NEW_VERSION_ID * 10 + "}";
        when(versionDiffRepository.searchVersionDiffIds(
                eq(OLD_VERSION_ID), eq(NEW_VERSION_ID), eq(versionIds)
        )).thenReturn(versionDiffIds);

        List<VersionDataDiffResult> diffs = List.of(
                createDiffResult(ROW_UPD_UPD_CODE, ROW_UPD_UPD_FIRST, ROW_UPD_UPD_LAST),
                createDiffResult(ROW_INS_CODE, ROW_INS_FIRST, null),
                createDiffResult(ROW_INS_DEL_CODE, ROW_INS_DEL_FIRST, ROW_INS_DEL_LAST)
        );

        // searchDataDiffs:
        VersionDataDiffCriteria criteria = new VersionDataDiffCriteria(OLD_VERSION_ID, NEW_VERSION_ID);
        when(dataDiffResultRepository.searchByVersionDiffs(
                eq(versionDiffIds), any(String.class), any(String.class), eq(criteria)
        )).thenReturn(new PageImpl<>(diffs, criteria, diffs.size()));

        List<VersionDataDiff> expected = diffs.stream().map(this::toVersionDataDiff).collect(toList());

        Page<VersionDataDiff> actual = service.search(criteria);
        assertNotNull(actual);
        assertListEquals(expected, actual.getContent());
    }

    /** Поиск в случае фильтрации по значениям первичных полей]. */
    @Test
    public void testSearchWhenPrimariesFiltered() {

        // getVersions:
        RefBookVersionEntity oldVersion = createVersionEntity(OLD_VERSION_ID);
        RefBookVersionEntity newVersion = createVersionEntity(NEW_VERSION_ID);
        when(versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                eq(List.of(OLD_VERSION_ID, NEW_VERSION_ID)), eq(RefBookVersionStatus.PUBLISHED)
        )).thenReturn(List.of(newVersion, oldVersion));

        // getVersionIds:
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode(TEST_REFBOOK_CODE);
        newVersion.setRefBook(refBookEntity);

        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(List.of(newVersion, oldVersion));
        String versionIds = NEW_VERSION_ID + "," + OLD_VERSION_ID;

        // searchVersionDiffIds:
        String versionDiffIds = "{" + OLD_VERSION_ID * 10 + ","  + NEW_VERSION_ID * 10 + "}";
        when(versionDiffRepository.searchVersionDiffIds(
                eq(OLD_VERSION_ID), eq(NEW_VERSION_ID), eq(versionIds)
        )).thenReturn(versionDiffIds);

        // searchDataDiffs:
        VersionDataDiffCriteria criteria = new VersionDataDiffCriteria(OLD_VERSION_ID, NEW_VERSION_ID);

        Set<List<AttributeFilter>> includeSet = new HashSet<>();
        AttributeFilter idFilter = new AttributeFilter(VERSION_ATTRIBUTE_ID, 1, FieldType.INTEGER, SearchTypeEnum.EXACT);
        AttributeFilter codeFilter = new AttributeFilter(VERSION_ATTRIBUTE_CODE, "1", FieldType.STRING, SearchTypeEnum.EXACT);
        includeSet.add(asList(idFilter, codeFilter));
        idFilter = new AttributeFilter(VERSION_ATTRIBUTE_ID, 2, FieldType.INTEGER, SearchTypeEnum.EXACT);
        codeFilter = new AttributeFilter(VERSION_ATTRIBUTE_CODE, "2", FieldType.STRING, SearchTypeEnum.EXACT);
        includeSet.add(asList(idFilter, codeFilter));
        criteria.setPrimaryAttributesFilters(includeSet);

        List<String> excludeList = asList("code=\"1\", id=1", "code=\"2\", id=2");
        criteria.setExcludePrimaryValues(excludeList);

        ArgumentCaptor<String> includeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> excludeCaptor = ArgumentCaptor.forClass(String.class);

        when(dataDiffResultRepository.searchByVersionDiffs(
                eq(versionDiffIds),
                includeCaptor.capture(),
                excludeCaptor.capture(),
                eq(criteria)
        )).thenReturn(new PageImpl<>(emptyList(), criteria, 0));

        Page<VersionDataDiff> actual = service.search(criteria);
        assertNotNull(actual);

        List<String> expectedList = asList("\"code=\\\"1\\\", id=1\"", "\"code=\\\"2\\\", id=2\"");

        assertNotNull(includeCaptor.getValue());
        assertNotNull(excludeCaptor.getValue());
        expectedList.forEach(expected -> {
            assertTrue(includeCaptor.getValue().contains(expected));
            assertTrue(excludeCaptor.getValue().contains(expected));
        });
    }

    /** Поиск в случае, когда не находится цепочка сохранённых разниц между версиями. */
    @Test
    public void testSearchWithVersionDiffGap() {

        // getVersions:
        RefBookVersionEntity oldVersion = createVersionEntity(OLD_VERSION_ID);
        RefBookVersionEntity newVersion = createVersionEntity(NEW_VERSION_ID);
        when(versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                eq(List.of(OLD_VERSION_ID, NEW_VERSION_ID)), eq(RefBookVersionStatus.PUBLISHED)
        )).thenReturn(List.of(newVersion, oldVersion));

        // getVersionIds:
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode(TEST_REFBOOK_CODE);
        newVersion.setRefBook(refBookEntity);

        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(List.of(newVersion, oldVersion));
        String versionIds = NEW_VERSION_ID + "," + OLD_VERSION_ID;

        // searchVersionDiffIds:
        when(versionDiffRepository.searchVersionDiffIds(
                eq(OLD_VERSION_ID), eq(NEW_VERSION_ID), eq(versionIds)
        )).thenReturn("");

        VersionDataDiffCriteria criteria = new VersionDataDiffCriteria(OLD_VERSION_ID, NEW_VERSION_ID);
        try {
            service.search(criteria);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("compare.data.diff.not.found", getExceptionMessage(e));
        }
    }

    private VersionDataDiffResult createDiffResult(String primaryValues, String firstDiffValues, String lastDiffValues) {

        VersionDataDiffResult result = new VersionDataDiffResult();
        result.setPrimaryValues(primaryValues);
        result.setFirstDiffValues(firstDiffValues);
        result.setLastDiffValues(lastDiffValues);

        return result;
    }

    private VersionDataDiff toVersionDataDiff(VersionDataDiffResult diff) {

        return new VersionDataDiff(
                diff.getPrimaryValues(),
                JsonUtil.fromJsonString(diff.getFirstDiffValues(), DiffRowValue.class),
                (diff.getLastDiffValues() != null)
                        ? JsonUtil.fromJsonString(diff.getLastDiffValues(), DiffRowValue.class)
                        : null
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveLastVersionDataDiff() {

        RefBookVersionDiffEntity versionDiffEntity = createVersionDiffEntity();
        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(List.of(versionDiffEntity.getNewVersion(), versionDiffEntity.getOldVersion()));

        RefBookVersionDiffEntity savedDiffEntity = createSavedDiffEntity(versionDiffEntity);
        when(versionDiffRepository.saveAndFlush(eq(versionDiffEntity))).thenReturn(savedDiffEntity);

        RefBookAttributeDiff attributeDiff = new RefBookAttributeDiff(
                singletonList(VERSION_ATTRIBUTE_DELETED),
                singletonList(VERSION_ATTRIBUTE_INSERTED),
                singletonList(VERSION_ATTRIBUTE_UPDATED)
        );

        Field idField = new IntegerField(VERSION_ATTRIBUTE_ID);
        Field codeField = new StringField(VERSION_ATTRIBUTE_CODE);
        Field nameField = new StringField(VERSION_ATTRIBUTE_NAME);
        Field amountField = new IntegerField(VERSION_ATTRIBUTE_AMOUNT);
        Field boolField = new BooleanField(VERSION_ATTRIBUTE_BOOL);

        DiffRowValue updatedValue = new DiffRowValue(asList(
                new DiffFieldValue(idField, null, BigInteger.valueOf(1L), null), // not changed
                new DiffFieldValue(codeField, null, "1", null), // not changed
                new DiffFieldValue(nameField, "def", "upd", DiffStatusEnum.UPDATED),
                new DiffFieldValue(amountField, BigInteger.valueOf(11L), null, DiffStatusEnum.DELETED),
                new DiffFieldValue(boolField, null, Boolean.FALSE, DiffStatusEnum.INSERTED)
        ), DiffStatusEnum.UPDATED);

        DiffRowValue insertedValue = new DiffRowValue(asList(
                new DiffFieldValue(idField, null, BigInteger.valueOf(2L), DiffStatusEnum.INSERTED),
                new DiffFieldValue(codeField, null, "2", DiffStatusEnum.INSERTED),
                new DiffFieldValue(nameField, null, "ins", DiffStatusEnum.INSERTED),
                new DiffFieldValue(amountField, null, BigInteger.valueOf(22L), DiffStatusEnum.INSERTED),
                new DiffFieldValue(boolField, null, Boolean.FALSE, DiffStatusEnum.INSERTED)
        ), DiffStatusEnum.INSERTED);

        DiffRowValue deletedValue = new DiffRowValue(asList(
                new DiffFieldValue(idField, BigInteger.valueOf(3L), null, DiffStatusEnum.DELETED),
                new DiffFieldValue(codeField, "3", null, DiffStatusEnum.DELETED),
                new DiffFieldValue(nameField, "del", null, DiffStatusEnum.DELETED),
                new DiffFieldValue(amountField, BigInteger.valueOf(33L), null, DiffStatusEnum.DELETED),
                new DiffFieldValue(boolField, Boolean.FALSE, null, DiffStatusEnum.DELETED)
        ), DiffStatusEnum.DELETED);

        Criteria vdsCriteria = new Criteria();
        vdsCriteria.setPage(RestCriteria.FIRST_PAGE_NUMBER + 1);
        vdsCriteria.setSize(100);

        final int diffRowValueCount = 3;

        CollectionPage<DiffRowValue> vdsDiffRowValuePage = new CollectionPage<>(diffRowValueCount,
                asList(updatedValue, insertedValue, deletedValue), vdsCriteria);

        RefBookDataDiff dataDiff = new RefBookDataDiff(new DiffRowValuePage(vdsDiffRowValuePage), attributeDiff);

        CollectionPage<DiffRowValue> emptyDiffRowValuePage = new CollectionPage<>(diffRowValueCount, emptyList(), vdsCriteria);

        when(compareService.compareData(any())).thenAnswer(invocation -> {
            CompareDataCriteria criteria = (CompareDataCriteria) (invocation.getArguments()[0]);
            return (criteria.getPageNumber() == RestCriteria.FIRST_PAGE_NUMBER)
                    ? dataDiff
                    : new RefBookDataDiff(new DiffRowValuePage(emptyDiffRowValuePage), attributeDiff);
        });

        service.saveLastVersionDataDiff(TEST_REFBOOK_CODE);

        verify(versionDiffRepository, times(1)).saveAndFlush(eq(versionDiffEntity));

        ArgumentCaptor<List<VersionDataDiffEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataDiffRepository, times(1)).saveAll(captor.capture());

        List<VersionDataDiffEntity> dataDiffEntities = captor.getValue();
        assertFalse(isEmpty(dataDiffEntities));
        assertEquals(diffRowValueCount, dataDiffEntities.size());

        assertTrue(dataDiffEntities.stream()
                .map(VersionDataDiffEntity::getVersionDiffEntity)
                .allMatch(savedDiffEntity::equals)
        );

        assertListEquals(asList("code=\"1\", id=1", "code=\"2\", id=2", "code=\"3\", id=3"),
                dataDiffEntities.stream().map(VersionDataDiffEntity::getPrimaries).collect(toList())
        );

        String dataDiffValues = dataDiffEntities.get(0).getValues();
        assertTrue(Stream.of(codeField, nameField, amountField, boolField)
                .map(field -> field.getClass().getSimpleName())
                .allMatch(dataDiffValues::contains)
        );
    }

    @Test
    public void testSaveLastVersionDataDiffWhenDataDiffFail() {

        RefBookVersionDiffEntity versionDiffEntity = createVersionDiffEntity();
        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(List.of(versionDiffEntity.getNewVersion(), versionDiffEntity.getOldVersion()));

        RefBookVersionDiffEntity savedDiffEntity = createSavedDiffEntity(versionDiffEntity);
        when(versionDiffRepository.saveAndFlush(eq(versionDiffEntity))).thenReturn(savedDiffEntity);

        // Имитация произвольной ошибки при работе с разницей между данными.
        when(compareService.compareData(any())).thenThrow(new NotFoundException(TEST_REFBOOK_CODE));

        try {
            service.saveLastVersionDataDiff(TEST_REFBOOK_CODE);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(TEST_REFBOOK_CODE, getExceptionMessage(e));
        }

        verify(versionDiffRepository, times(1)).saveAndFlush(eq(versionDiffEntity));

        verify(versionDiffRepository, times(1)).delete(eq(savedDiffEntity));
        verify(versionDiffRepository, times(1)).flush();
    }

    @Test
    public void testSaveLastVersionDataDiffWhenCodeNotExist() {

        doThrow(new NotFoundException(TEST_REFBOOK_CODE))
                .when(versionValidation).validateRefBookCodeExists(eq(TEST_REFBOOK_CODE));

        try {
            service.saveLastVersionDataDiff(TEST_REFBOOK_CODE);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(TEST_REFBOOK_CODE, getExceptionMessage(e));
        }
    }

    @Test
    public void testSaveLastVersionDataDiffWhenNoVersions() {

        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(emptyList());

        try {
            service.saveLastVersionDataDiff(TEST_REFBOOK_CODE);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
            assertTrue(getExceptionMessage(e).contains(TEST_REFBOOK_CODE));
        }
    }

    @Test
    public void testSaveLastVersionDataDiffWhenOneVersionOnly() {

        RefBookVersionEntity entity = new RefBookVersionEntity();

        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(singletonList(entity));

        try {
            service.saveLastVersionDataDiff(TEST_REFBOOK_CODE);

        } catch (RuntimeException e) {
            fail("No error expected");
        }
    }

    @Test
    public void testIsPublishedBeforeWhenTrue() {

        // getVersions:
        RefBookVersionEntity oldVersion = createVersionEntity(OLD_VERSION_ID);
        RefBookVersionEntity newVersion = createVersionEntity(NEW_VERSION_ID);
        when(versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                eq(List.of(OLD_VERSION_ID, NEW_VERSION_ID)), eq(RefBookVersionStatus.PUBLISHED)
        )).thenReturn(List.of(newVersion, oldVersion));

        Boolean result = service.isPublishedBefore(OLD_VERSION_ID, NEW_VERSION_ID);
        assertTrue(result);
    }

    @Test
    public void testIsPublishedBeforeWhenFalse() {

        // getVersions:
        RefBookVersionEntity oldVersion = createVersionEntity(OLD_VERSION_ID);
        RefBookVersionEntity newVersion = createVersionEntity(NEW_VERSION_ID);
        when(versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                eq(List.of(OLD_VERSION_ID, NEW_VERSION_ID)), eq(RefBookVersionStatus.PUBLISHED)
        )).thenReturn(List.of(oldVersion, newVersion));

        Boolean result = service.isPublishedBefore(OLD_VERSION_ID, NEW_VERSION_ID);
        assertFalse(result);
    }

    @Test
    public void testIsPublishedBeforeWhenOldIsNull() {

        try {
            service.isPublishedBefore(null, NEW_VERSION_ID);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("version.not.found", getExceptionMessage(e));
        }

        verify(versionRepository, never()).findByIdInAndStatusOrderByFromDateDesc(any(), eq(RefBookVersionStatus.PUBLISHED));
    }

    @Test
    public void testIsPublishedBeforeWhenOldNotExists() {

        RefBookVersionEntity newVersion = createVersionEntity(NEW_VERSION_ID);

        when(versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                eq(List.of(OLD_VERSION_ID, NEW_VERSION_ID)), eq(RefBookVersionStatus.PUBLISHED)
        )).thenReturn(singletonList(newVersion));

        try {
            service.isPublishedBefore(OLD_VERSION_ID, NEW_VERSION_ID);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("version.not.found", getExceptionMessage(e));
        }
    }

    @Test
    public void testIsPublishedBeforeWhenNewIsNull() {

        try {
            service.isPublishedBefore(OLD_VERSION_ID, null);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("version.not.found", getExceptionMessage(e));
        }

        verify(versionRepository, never()).findByIdInAndStatusOrderByFromDateDesc(any(), eq(RefBookVersionStatus.PUBLISHED));
    }

    @Test
    public void testIsPublishedBeforeWhenNewNotExists() {

        RefBookVersionEntity oldVersion = createVersionEntity(OLD_VERSION_ID);

        when(versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                eq(List.of(OLD_VERSION_ID, NEW_VERSION_ID)), eq(RefBookVersionStatus.PUBLISHED)
        )).thenReturn(singletonList(oldVersion));

        try {
            service.isPublishedBefore(OLD_VERSION_ID, NEW_VERSION_ID);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("version.not.found", getExceptionMessage(e));
        }
    }

    private RefBookVersionDiffEntity createVersionDiffEntity() {

        RefBookVersionEntity oldVersion = createVersionEntity(OLD_VERSION_ID);
        oldVersion.setStructure(createOldStructure());
        RefBookVersionEntity newVersion = createVersionEntity(NEW_VERSION_ID);
        newVersion.setStructure(createNewStructure());

        return new RefBookVersionDiffEntity(oldVersion, newVersion);
    }

    private RefBookVersionDiffEntity createSavedDiffEntity(RefBookVersionDiffEntity versionDiffEntity) {

        RefBookVersionDiffEntity result = new RefBookVersionDiffEntity(
                versionDiffEntity.getOldVersion(), versionDiffEntity.getNewVersion());
        result.setId(VERSION_DIFF_ID);

        return result;
    }

    private RefBookVersionEntity createVersionEntity(Integer id) {

        RefBookVersionEntity result = new RefBookVersionEntity();
        result.setId(id);

        return result;
    }

    private Structure createOldStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.buildPrimary(VERSION_ATTRIBUTE_ID, "Ид-р", FieldType.INTEGER, "идентификатор"),
                        Structure.Attribute.buildPrimary(VERSION_ATTRIBUTE_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_NAME, "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_AMOUNT, "Количество", FieldType.INTEGER, "количество единиц"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_BOOL, "Признак", FieldType.BOOLEAN, "логическое значение"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_UPDATED, "Изменяемый", FieldType.STRING, "исходный строковый"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_DELETED, "Старый", FieldType.STRING, "удаляемый атрибут")
                ),
                emptyList()
        );
    }

    private Structure createNewStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.buildPrimary(VERSION_ATTRIBUTE_ID, "Ид-р", FieldType.INTEGER, "идентификатор"),
                        Structure.Attribute.buildPrimary(VERSION_ATTRIBUTE_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_NAME, "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_AMOUNT, "Количество", FieldType.INTEGER, "количество единиц"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_BOOL, "Признак", FieldType.BOOLEAN, "логическое значение"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_UPDATED, "Изменённый", FieldType.INTEGER, "исходный целочисленный"),
                        Structure.Attribute.build(VERSION_ATTRIBUTE_INSERTED, "Новый", FieldType.STRING, "добавляемый атрибут")
                ),
                emptyList()
        );
    }
}