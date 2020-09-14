package ru.i_novus.ms.rdm.n2o.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тестирование работы с данными справочника.
 */
@RunWith(MockitoJUnitRunner.class)
public class RefBookDataServiceTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;

    private static final String ATTRIBUTE_ID_CODE = "id";
    private static final String ATTRIBUTE_NAME_CODE = "name";
    private static final String ATTRIBUTE_TEXT_CODE = "text";

    @InjectMocks
    RefBookDataServiceImpl refBookDataService;

    @Mock
    private VersionRestService versionService;

    @Test
    public void testGetDataStructure() {

        Structure structure = createStructure();
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(structure);

        Structure dataStructure = refBookDataService.getDataStructure(TEST_REFBOOK_VERSION_ID, null);
        assertEquals(structure, dataStructure);
    }

    @Test
    public void testGetDataContent() {

        List<RefBookRowValue> searchContent = createContent(TEST_REFBOOK_VERSION_ID);

        List<RefBookRowValue> dataContent = refBookDataService.getDataContent(searchContent, null);
        assertEquals(searchContent, dataContent);
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
                    new StringFieldValue(ATTRIBUTE_TEXT_CODE, "text with id = " + systemId)
            ));
            rowValues.add(new RefBookRowValue(longRowValue, versionId));
        });

        return rowValues;
    }
}
