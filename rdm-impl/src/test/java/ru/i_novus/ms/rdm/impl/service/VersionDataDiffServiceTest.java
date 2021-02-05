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
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.DiffRowValuePage;
import ru.i_novus.ms.rdm.api.model.diff.RefBookAttributeDiff;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.provider.RdmMapperConfigurer;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.RefBookVersionDiffEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.RefBookVersionDiffRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.VersionDataDiffRepository;
import ru.i_novus.ms.rdm.test.BaseTest;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.BooleanField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import java.math.BigInteger;
import java.util.List;
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
@SuppressWarnings("java:S5778")
public class VersionDataDiffServiceTest extends BaseTest {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String TEST_REFBOOK_CODE = "test_code";

    private static final String VERSION_ATTRIBUTE_CODE = "code";
    private static final String VERSION_ATTRIBUTE_NAME = "name";
    private static final String VERSION_ATTRIBUTE_AMOUNT = "amount";
    private static final String VERSION_ATTRIBUTE_BOOL = "bool";
    private static final String VERSION_ATTRIBUTE_UPDATED = "updated";
    private static final String VERSION_ATTRIBUTE_INSERTED = "inserted";
    private static final String VERSION_ATTRIBUTE_DELETED = "deleted";

    @InjectMocks
    private VersionDataDiffServiceImpl service;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookVersionDiffRepository versionDiffRepository;
    @Mock
    private VersionDataDiffRepository dataDiffRepository;

    @Mock
    private CompareService compareService;

    @Mock
    private VersionValidation versionValidation;

    @Before
    public void setUp() throws NoSuchFieldException {

        JsonUtil.jsonMapper = JSON_MAPPER;

        new RdmMapperConfigurer().configure(JSON_MAPPER);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveLastVersionDataDiff() {

        RefBookVersionEntity oldVersion = new RefBookVersionEntity();
        oldVersion.setId(1);
        oldVersion.setStructure(createOldStructure());
        RefBookVersionEntity newVersion = new RefBookVersionEntity();
        newVersion.setId(2);
        newVersion.setStructure(createNewStructure());

        when(versionRepository.findByRefBookCodeAndStatusOrderByFromDateDesc(
                eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED), any()
        )).thenReturn(List.of(newVersion, oldVersion));

        RefBookVersionDiffEntity versionDiffEntity = new RefBookVersionDiffEntity(oldVersion, newVersion);
        RefBookVersionDiffEntity savedDiffEntity = new RefBookVersionDiffEntity(oldVersion, newVersion);
        savedDiffEntity.setId(3);

        when(versionDiffRepository.save(eq(versionDiffEntity))).thenReturn(savedDiffEntity);

        RefBookAttributeDiff attributeDiff = new RefBookAttributeDiff(
                singletonList(VERSION_ATTRIBUTE_DELETED),
                singletonList(VERSION_ATTRIBUTE_INSERTED),
                singletonList(VERSION_ATTRIBUTE_UPDATED)
        );

        Field codeField = new StringField(VERSION_ATTRIBUTE_CODE);
        Field nameField = new StringField(VERSION_ATTRIBUTE_NAME);
        Field amountField = new IntegerField(VERSION_ATTRIBUTE_AMOUNT);
        Field boolField = new BooleanField(VERSION_ATTRIBUTE_BOOL);

        DiffRowValue updatedValue = new DiffRowValue(
                asList(
                        new DiffFieldValue(codeField, null, "1", null), // not changed
                        new DiffFieldValue(nameField, "def", "upd", DiffStatusEnum.UPDATED),
                        new DiffFieldValue(amountField, BigInteger.valueOf(11L), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue(boolField, null, Boolean.FALSE, DiffStatusEnum.INSERTED)
                ),
                DiffStatusEnum.UPDATED
        );

        DiffRowValue insertedValue = new DiffRowValue(
                asList(
                        new DiffFieldValue(codeField, null, "2", DiffStatusEnum.INSERTED),
                        new DiffFieldValue(nameField, null, "ins", DiffStatusEnum.INSERTED),
                        new DiffFieldValue(amountField, null, BigInteger.valueOf(22L), DiffStatusEnum.INSERTED),
                        new DiffFieldValue(boolField, null, Boolean.FALSE, DiffStatusEnum.INSERTED)
                ),
                DiffStatusEnum.INSERTED
        );

        DiffRowValue deletedValue = new DiffRowValue(
                asList(
                        new DiffFieldValue(codeField, "3", null, DiffStatusEnum.DELETED),
                        new DiffFieldValue(nameField, "del", null, DiffStatusEnum.DELETED),
                        new DiffFieldValue(amountField, BigInteger.valueOf(33L), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue(boolField, Boolean.FALSE, null, DiffStatusEnum.DELETED)
                ),
                DiffStatusEnum.DELETED
        );

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

        verify(versionDiffRepository, times(1)).save(eq(versionDiffEntity));

        ArgumentCaptor<List<VersionDataDiffEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataDiffRepository, times(1)).saveAll(captor.capture());

        List<VersionDataDiffEntity> dataDiffEntities = captor.getValue();
        assertFalse(isEmpty(dataDiffEntities));
        assertEquals(diffRowValueCount, dataDiffEntities.size());

        assertTrue(dataDiffEntities.stream()
                .map(VersionDataDiffEntity::getVersionDiffEntity)
                .allMatch(savedDiffEntity::equals)
        );

        assertListEquals(asList("code=\"1\"", "code=\"2\"", "code=\"3\""),
                dataDiffEntities.stream().map(VersionDataDiffEntity::getPrimaries).collect(toList())
        );

        String dataDiffValues = dataDiffEntities.get(0).getValues();
        assertTrue(Stream.of(codeField, nameField, amountField, boolField)
                .map(field -> field.getClass().getSimpleName())
                .allMatch(dataDiffValues::contains)
        );
    }

    private Structure createOldStructure() {
        return new Structure(
                asList(
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
}