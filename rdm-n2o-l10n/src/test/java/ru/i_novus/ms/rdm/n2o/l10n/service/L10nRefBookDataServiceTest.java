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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тестирование работы с данными справочника.
 */
@RunWith(MockitoJUnitRunner.class)
public class L10nRefBookDataServiceTest {

    private static final String SYS_LOCALIZED = "SYS_LOCALIZED"; // from l10n-vds L10nConstants

    private static final int TEST_REFBOOK_VERSION_ID = -10;

    private static final String TEST_LOCALE_CODE = "test";

    private static final String ATTRIBUTE_ID_CODE = "id";
    private static final String ATTRIBUTE_NAME_CODE = "name";
    private static final String ATTRIBUTE_TEXT_CODE = "text";

    @InjectMocks
    L10nRefBookDataServiceImpl refBookDataService;

    @Mock
    private VersionRestService versionService;

    @Mock
    private Messages messages;

    @Test
    public void testGetDataStructure() {

        Structure structure = createStructure();
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(structure);

        Structure dataStructure = refBookDataService.getDataStructure(TEST_REFBOOK_VERSION_ID, null);
        assertEquals(structure, dataStructure);

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        dataStructure = refBookDataService.getDataStructure(TEST_REFBOOK_VERSION_ID, createLocaleCriteria());
        assertEquals(structure.getAttributes().size() + 1, dataStructure.getAttributes().size());

        assertTrue(dataStructure.getAttributes().stream().anyMatch(attribute -> SYS_LOCALIZED.equals(attribute.getCode())));
        Structure originStructure = new Structure(dataStructure);
        originStructure.getAttributes().removeIf(attribute -> SYS_LOCALIZED.equals(attribute.getCode()));
        assertEquals(structure, originStructure);
    }

    @Test
    public void testGetDataContent() {

        List<RefBookRowValue> searchContent = createContent(TEST_REFBOOK_VERSION_ID);

        List<RefBookRowValue> dataContent = refBookDataService.getDataContent(searchContent, null);
        assertEquals(searchContent, dataContent);

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        dataContent = refBookDataService.getDataContent(searchContent, createLocaleCriteria());
        assertEquals(searchContent.size(), dataContent.size());
        assertEquals(searchContent, dataContent);
        assertTrue(dataContent.stream().allMatch(
                rowValue -> rowValue.getFieldValues().stream()
                        .anyMatch(fieldValue -> SYS_LOCALIZED.equals(fieldValue.getField()) &&
                                fieldValue instanceof StringFieldValue)
        ));
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

        int rowValueCount = 10;

        List<RefBookRowValue> rowValues = new ArrayList<>(rowValueCount);

        LongStream.range(0, rowValueCount).forEach(systemId -> {
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

        DataCriteria criteria = new DataCriteria();
        criteria.setLocaleCode(TEST_LOCALE_CODE);

        return criteria;
    }
}
