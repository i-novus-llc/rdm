package ru.inovus.ms.rdm.sync;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmLoggingService;
import ru.inovus.ms.rdm.sync.service.RdmMappingService;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncRestImpl;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lgalimova
 * @since 26.02.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class RdmSyncRestTest {
    private static final int MAX_SIZE = 100;

    @InjectMocks
    private RdmSyncRestImpl rdmSyncRest;
    @Mock
    private RdmSyncDao dao;
    @Mock
    private RefBookService refBookService;
    @Mock
    private VersionService versionService;
    @Mock
    private CompareService compareService;
    @Mock
    private RdmMappingService mappingService;
    @Mock
    private RdmLoggingService rdmLoggingService;

    /**
     * Кейс: Обновление справочника в первый раз, версия в маппинге не указана. В таблице клиента уже есть запись с id=1, из НСИ приходят записи с id=1,2.
     * Ожидаемый результат: Запись с id=1 обновится, с id=2 вставится, в маппинге проставится дата и номер версии.
     */
    @Test
    public void testFirstTimeUpdate() {
        RefBook firstVersion = createFirstRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", null, null, "test_table", "id", "is_deleted");
        List<FieldMapping> fieldMappings = createFieldMappings();
        FieldMapping primaryFieldMapping = fieldMappings.stream().filter(f -> f.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElse(null);
        Page<RefBookRowValue> data = createFirstRdmData();
        List<Map<String, Object>> dataMap = createFirstVerifyDataMap();

        when(dao.getVersionMapping(versionMapping.getCode())).thenReturn(versionMapping);
        when(dao.getFieldMapping(versionMapping.getCode())).thenReturn(fieldMappings);
        when(dao.getDataIds(versionMapping.getTable(), primaryFieldMapping)).thenReturn(Collections.singletonList(BigInteger.valueOf(1L)));
        when(refBookService.search(any(RefBookCriteria.class))).thenReturn(new PageImpl<>(Collections.singletonList(firstVersion), PageRequest.of(0, 10), 1L));
        when(versionService.search(eq(versionMapping.getCode()), any(SearchDataCriteria.class))).thenReturn(data);
        when(mappingService.map(FieldType.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(0).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(1L));
        when(mappingService.map(FieldType.STRING, DataTypeEnum.VARCHAR, data.getContent().get(0).getFieldValues().get(1).getValue())).thenReturn("London");
        when(mappingService.map(FieldType.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(1).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(2L));
        when(mappingService.map(FieldType.STRING, DataTypeEnum.VARCHAR, data.getContent().get(1).getFieldValues().get(1).getValue())).thenReturn("Moscow");
        rdmSyncRest.update(versionMapping.getCode());
        verify(dao).updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), dataMap.get(0));
        verify(dao).insertRow(versionMapping.getTable(), dataMap.get(1));
        verify(dao).updateVersionMapping(versionMapping.getId(), firstVersion.getLastPublishedVersion(), firstVersion.getLastPublishedVersionFromDate());
    }

    /**
     * Кейс: Обновление справочника c уже указанной версией в маппинге. В таблице клиента уже есть запись с id=1,2. Из НСИ приходят записи с id=2,3.
     * Ожидаемый результат: Запись с id=1 пометится как удаленная, с id=3 добавится. В маппинге проставится дата и номер новой версии.
     */
    @Test
    public void testUpdate() {
        RefBook firstVersion = createFirstRdmVersion();
        RefBook secondVersion = createSecondRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", firstVersion.getLastPublishedVersion(), firstVersion.getLastPublishedVersionFromDate(), "test_table", "id", "is_deleted");
        List<FieldMapping> fieldMappings = createFieldMappings();
        Page<RefBookRowValue> data = createSecondRdmData();
        List<Map<String, Object>> dataMap = createSecondVerifyDataMap();
        RefBookDataDiff diff = prepareUpdateRefBookDataDiff();

        when(dao.getVersionMapping(versionMapping.getCode())).thenReturn(versionMapping);
        when(dao.getFieldMapping(versionMapping.getCode())).thenReturn(fieldMappings);
        when(versionService.getVersion(versionMapping.getVersion(), versionMapping.getCode())).thenReturn(firstVersion);
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(diff);
        when(refBookService.search(any(RefBookCriteria.class))).thenReturn(new PageImpl<>(Collections.singletonList(secondVersion), PageRequest.of(0, 10), 1L));
        when(mappingService.map(FieldType.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(0).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(1L));
        when(mappingService.map(FieldType.STRING, DataTypeEnum.VARCHAR, data.getContent().get(0).getFieldValues().get(1).getValue())).thenReturn("London");
        when(mappingService.map(FieldType.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(2).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(3L));
        when(mappingService.map(FieldType.STRING, DataTypeEnum.VARCHAR, data.getContent().get(2).getFieldValues().get(1).getValue())).thenReturn("Guadalupe");
        rdmSyncRest.update(versionMapping.getCode());
        verify(dao).markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), BigInteger.valueOf(1L), true);
        verify(dao).insertRow(versionMapping.getTable(), dataMap.get(1));
        verify(dao).updateVersionMapping(versionMapping.getId(), secondVersion.getLastPublishedVersion(), secondVersion.getLastPublishedVersionFromDate());
    }

    /**
     * Кейс: Обновление справочника c уже указанной версией в маппинге. В таблице клиента уже есть запись с id=1,2,3. 1 помечена как удаленная. Из НСИ приходят записи с id=1,2,3.
     * Ожидаемый результат: У записи id=1 должен сняться признак удаления. В маппинге проставится дата и номер новой версии.
     */
    @Test
    public void testInsert() {
        RefBook oldVersion = createSecondRdmVersion();
        RefBook newVersion = createThirdRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", oldVersion.getLastPublishedVersion(), oldVersion.getLastPublishedVersionFromDate(), "test_table", "id", "is_deleted");
        List<FieldMapping> fieldMappings = createFieldMappings();
        Page<RefBookRowValue> data = createThirdRdmData();
        List<Map<String, Object>> dataMap = createThirdVerifyDataMap();
        RefBookDataDiff diff = prepareInsertRefBookDataDiff();
        when(dao.getVersionMapping(versionMapping.getCode())).thenReturn(versionMapping);
        when(dao.getFieldMapping(versionMapping.getCode())).thenReturn(fieldMappings);
        when(versionService.getVersion(versionMapping.getVersion(), versionMapping.getCode())).thenReturn(oldVersion);
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(diff);
        when(refBookService.search(any(RefBookCriteria.class))).thenReturn(new PageImpl<>(Collections.singletonList(newVersion), PageRequest.of(0, 10), 1L));
        when(mappingService.map(FieldType.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(0).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(1L));
        when(mappingService.map(FieldType.STRING, DataTypeEnum.VARCHAR, data.getContent().get(0).getFieldValues().get(1).getValue())).thenReturn("London");
        when(dao.isIdExists(versionMapping.getTable(), versionMapping.getPrimaryField(), BigInteger.ONE)).thenReturn(true);
        rdmSyncRest.update(versionMapping.getCode());
        verify(dao).markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), BigInteger.valueOf(1L), false);
        verify(dao).updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), dataMap.get(2));
        verify(dao).updateVersionMapping(versionMapping.getId(), newVersion.getLastPublishedVersion(), newVersion.getLastPublishedVersionFromDate());
    }

    private RefBook createFirstRdmVersion() {
        RefBook refBook = new RefBook();
        refBook.setLastPublishedVersion("1.0");
        refBook.setLastPublishedVersionFromDate(LocalDateTime.of(2019, Month.FEBRUARY, 26, 10, 0));
        Structure.Attribute idAttribute = Structure.Attribute.build("id", null, FieldType.INTEGER, null);
        Structure.Attribute nameAttribute = Structure.Attribute.build("name", null, FieldType.STRING, null);
        idAttribute.setPrimary(true);
        refBook.setStructure(new Structure(asList(idAttribute, nameAttribute), null));
        return refBook;
    }

    private RefBook createSecondRdmVersion() {
        RefBook refBook = new RefBook();
        refBook.setLastPublishedVersion("1.1");
        refBook.setLastPublishedVersionFromDate(LocalDateTime.of(2019, Month.FEBRUARY, 27, 10, 0));
        Structure.Attribute idAttribute = Structure.Attribute.build("id", null, FieldType.INTEGER, null);
        Structure.Attribute nameAttribute = Structure.Attribute.build("name", null, FieldType.STRING, null);
        idAttribute.setPrimary(true);
        refBook.setStructure(new Structure(asList(idAttribute, nameAttribute), null));
        return refBook;
    }

    private RefBook createThirdRdmVersion() {
        RefBook refBook = new RefBook();
        refBook.setLastPublishedVersion("1.2");
        refBook.setLastPublishedVersionFromDate(LocalDateTime.of(2019, Month.MARCH, 7, 10, 0));
        Structure.Attribute idAttribute = Structure.Attribute.build("id", null, FieldType.INTEGER, null);
        Structure.Attribute nameAttribute = Structure.Attribute.build("name", null, FieldType.STRING, null);
        idAttribute.setPrimary(true);
        refBook.setStructure(new Structure(asList(idAttribute, nameAttribute), null));
        return refBook;
    }

    private RefBookDataDiff prepareUpdateRefBookDataDiff() {
        DiffFieldValue<BigInteger> id1 = new DiffFieldValue<>(new CommonField("id"), BigInteger.ONE, null, DiffStatusEnum.DELETED);
        DiffFieldValue<String> name1 = new DiffFieldValue<>(new CommonField("name"), "London", null, DiffStatusEnum.DELETED);
        DiffFieldValue<BigInteger> id2 = new DiffFieldValue<>(new CommonField("id"), null, BigInteger.valueOf(3L), DiffStatusEnum.INSERTED);
        DiffFieldValue<String> name2 = new DiffFieldValue<>(new CommonField("name"), null, "Guadalupe", DiffStatusEnum.INSERTED);
        DiffRowValue row1 = new DiffRowValue(asList(id1, name1), DiffStatusEnum.DELETED);
        DiffRowValue row2 = new DiffRowValue(asList(id2, name2), DiffStatusEnum.INSERTED);
        List<DiffRowValue> rowValues = asList(row1, row2);
        RefBookDataDiff diff = new RefBookDataDiff();
        diff.setRows(new PageImpl<>(rowValues, createSearchDataCriteria(), 2));
        return diff;
    }

    private RefBookDataDiff prepareInsertRefBookDataDiff() {
        DiffFieldValue<BigInteger> id = new DiffFieldValue<>(new CommonField("id"), null, BigInteger.ONE, DiffStatusEnum.INSERTED);
        DiffFieldValue<String> name = new DiffFieldValue<>(new CommonField("name"),null, "London", DiffStatusEnum.INSERTED);
        DiffRowValue row = new DiffRowValue(asList(id, name), DiffStatusEnum.INSERTED);
        RefBookDataDiff diff = new RefBookDataDiff();
        diff.setRows(new PageImpl<>(Collections.singletonList(row), createSearchDataCriteria(), 1));
        return diff;
    }

    private SearchDataCriteria createSearchDataCriteria() {
        SearchDataCriteria searchDataCriteriaCount = new SearchDataCriteria();
        searchDataCriteriaCount.setPageSize(MAX_SIZE);
        return searchDataCriteriaCount;
    }

    private List<FieldMapping> createFieldMappings() {
        List<FieldMapping> list = new ArrayList<>();
        list.add(new FieldMapping("id", "bigint", "id"));
        list.add(new FieldMapping("full_name", "varchar", "name"));
        return list;
    }

    private Page<RefBookRowValue> createFirstRdmData() {
        List<RefBookRowValue> list = new ArrayList<>();
        list.add(new RefBookRowValue(1L, asList(new IntegerFieldValue("id", 1), new StringFieldValue("name", "London")), null));
        list.add(new RefBookRowValue(2L, asList(new IntegerFieldValue("id", 2), new StringFieldValue("name", "Moscow")), null));
        return new PageImpl<>(list, createSearchDataCriteria(), 2);
    }

    private Page<RefBookRowValue> createSecondRdmData() {
        List<RefBookRowValue> list = new ArrayList<>();
        list.add(new RefBookRowValue(1L, asList(new IntegerFieldValue("id", 1), new StringFieldValue("name", "London")), null));
        list.add(new RefBookRowValue(2L, asList(new IntegerFieldValue("id", 2), new StringFieldValue("name", "Moscow")), null));
        list.add(new RefBookRowValue(3L, asList(new IntegerFieldValue("id", 3), new StringFieldValue("name", "Guadalupe")), null));
        return new PageImpl<>(list, createSearchDataCriteria(), 3);
    }

    private Page<RefBookRowValue> createThirdRdmData() {
        List<RefBookRowValue> list = new ArrayList<>();
        list.add(new RefBookRowValue(1L, asList(new IntegerFieldValue("id", 1), new StringFieldValue("name", "London")), null));
        list.add(new RefBookRowValue(2L, asList(new IntegerFieldValue("id", 2), new StringFieldValue("name", "Moscow")), null));
        list.add(new RefBookRowValue(3L, asList(new IntegerFieldValue("id", 3), new StringFieldValue("name", "Guadalupe")), null));
        return new PageImpl<>(list, createSearchDataCriteria(), 3);
    }

    private List<Map<String, Object>> createFirstVerifyDataMap() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", BigInteger.valueOf(1L));
        row1.put("full_name", "London");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", BigInteger.valueOf(2L));
        row2.put("full_name", "Moscow");
        return asList(row1, row2);
    }

    private List<Map<String, Object>> createSecondVerifyDataMap() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", BigInteger.valueOf(2L));
        row1.put("full_name", "Moscow");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", BigInteger.valueOf(3L));
        row2.put("full_name", "Guadalupe");
        return asList(row1, row2);
    }

    private List<Map<String, Object>> createThirdVerifyDataMap() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", BigInteger.valueOf(2L));
        row1.put("full_name", "Moscow");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", BigInteger.valueOf(3L));
        row2.put("full_name", "Guadalupe");
        Map<String, Object> row3 = new HashMap<>();
        row3.put("id", BigInteger.valueOf(1L));
        row3.put("full_name", "London");
        return asList(row1, row2, row3);
    }

}
