package ru.i_novus.ms.rdm.n2o.l10n.service;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.l10n.BaseTest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.BooleanFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.SYS_LOCALIZED;

/**
 * Тестирование работы с данными справочника.
 */
@RunWith(MockitoJUnitRunner.class)
public class L10nRefBookDataDecoratorTest extends BaseTest {

    private static final int TEST_VERSION_ID = -10;

    private static final String TEST_LOCALE_CODE = "test";

    private static final String ATTRIBUTE_ID_CODE = "id";
    private static final String ATTRIBUTE_NAME_CODE = "name";
    private static final String ATTRIBUTE_TEXT_CODE = "text";

    @InjectMocks
    private L10nRefBookDataDecorator refBookDataService;

    @Mock
    private VersionRestService versionService;

    @Mock
    private Messages messages;

    @Test
    public void testGetDataStructure() {

        final Structure structure = createStructure();
        when(versionService.getStructure(eq(TEST_VERSION_ID))).thenReturn(structure);

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        final Structure dataStructure = refBookDataService.getDataStructure(TEST_VERSION_ID, createLocaleCriteria());
        assertEquals(structure.getAttributes().size() + 1, dataStructure.getAttributes().size());
        assertTrue(dataStructure.getAttributes().stream()
                .anyMatch(attribute -> SYS_LOCALIZED.equals(attribute.getCode())));

        final Structure originStructure = new Structure(dataStructure);
        originStructure.getAttributes().removeIf(attribute -> SYS_LOCALIZED.equals(attribute.getCode()));
        assertEquals(structure, originStructure);
    }

    @Test
    public void testGetDataStructureOnSpecials() {

        final Structure structure = createStructure();
        when(versionService.getStructure(eq(TEST_VERSION_ID))).thenReturn(structure);

        final Structure dataStructureOnNull = refBookDataService.getDataStructure(TEST_VERSION_ID, null);
        assertEquals(structure, dataStructureOnNull);

        final Structure dataStructureOnEmpty = refBookDataService.getDataStructure(TEST_VERSION_ID, new DataCriteria());
        assertEquals(structure, dataStructureOnEmpty);
    }

    @Test
    public void testGetDataStructureWhenNull() {

        when(versionService.getStructure(eq(TEST_VERSION_ID))).thenReturn(null);

        final Structure dataStructure = refBookDataService.getDataStructure(TEST_VERSION_ID, createLocaleCriteria());
        assertNull(dataStructure);
    }

    @Test
    public void testGetDataStructureWhenEmpty() {

        when(versionService.getStructure(eq(TEST_VERSION_ID))).thenReturn(Structure.EMPTY);

        final Structure dataStructure = refBookDataService.getDataStructure(TEST_VERSION_ID, createLocaleCriteria());
        assertEquals(Structure.EMPTY, dataStructure);
    }

    @Test
    public void testGetDataContent() {

        final List<RefBookRowValue> searchContent = createContent(TEST_VERSION_ID);

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        final List<RefBookRowValue> dataContent = refBookDataService.getDataContent(searchContent, createLocaleCriteria());
        assertEquals(searchContent.size(), dataContent.size());
        assertEquals(searchContent, dataContent);
        assertTrue(dataContent.stream().allMatch(
                rowValue -> rowValue.getFieldValues().stream()
                        .anyMatch(fieldValue -> SYS_LOCALIZED.equals(fieldValue.getField()) &&
                                fieldValue instanceof StringFieldValue)
        ));
    }

    @Test
    public void testGetDataContentOnSpecials() {

        final List<RefBookRowValue> emptyContent = refBookDataService.getDataContent(emptyList(), createLocaleCriteria());
        assertEmpty(emptyContent);

        final List<RefBookRowValue> searchContent = createContent(TEST_VERSION_ID);

        final List<RefBookRowValue> contentOnNull = refBookDataService.getDataContent(searchContent, null);
        assertEquals(searchContent, contentOnNull);

        final List<RefBookRowValue> contentOnEmpty = refBookDataService.getDataContent(searchContent, new DataCriteria());
        assertEquals(searchContent, contentOnEmpty);
    }

    private Structure createStructure() {

        return new Structure(asList(
                Structure.Attribute.buildPrimary(ATTRIBUTE_ID_CODE, "Идентификатор", FieldType.INTEGER, null),
                Structure.Attribute.build(ATTRIBUTE_NAME_CODE, "Наименование", FieldType.STRING, null),
                Structure.Attribute.build(ATTRIBUTE_TEXT_CODE, "Текст", FieldType.STRING, null)
        ), null);
    }

    @SuppressWarnings("SameParameterValue")
    private List<RefBookRowValue> createContent(int versionId) {

        final int rowValueCount = 10;
        final List<RefBookRowValue> rowValues = new ArrayList<>(rowValueCount);

        LongStream.range(1, rowValueCount + 1).forEach(systemId -> {
            LongRowValue longRowValue = new LongRowValue(systemId, asList(
                    new IntegerFieldValue(ATTRIBUTE_ID_CODE, BigInteger.valueOf(systemId)),
                    new StringFieldValue(ATTRIBUTE_NAME_CODE, "name_" + systemId),
                    new StringFieldValue(ATTRIBUTE_TEXT_CODE, "text with id = " + systemId),
                    new BooleanFieldValue(SYS_LOCALIZED, systemId >= rowValueCount / 2)
            ));
            rowValues.add(new RefBookRowValue(longRowValue, versionId));
        });

        return rowValues;
    }

    private DataCriteria createLocaleCriteria() {

        final DataCriteria criteria = new DataCriteria();
        criteria.setLocaleCode(TEST_LOCALE_CODE);

        return criteria;
    }
}
