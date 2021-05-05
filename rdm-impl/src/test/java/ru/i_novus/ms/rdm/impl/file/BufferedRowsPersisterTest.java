package ru.i_novus.ms.rdm.impl.file;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.impl.file.process.BufferedRowsPersister;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class BufferedRowsPersisterTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";

    private static final int BUFFER_SIZE = 2;

    private BufferedRowsPersister persister;

    private Field nameField;

    private Field countField;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private DraftDataService draftDataService;

    @Before
    public void setUp() {

        persister = new BufferedRowsPersister(draftDataService, TEST_STORAGE_CODE, createTestStructure(), BUFFER_SIZE);

        when(fieldFactory.createField(eq("name"), eq(FieldType.STRING))).thenReturn(new StringField("name"));
        when(fieldFactory.createField(eq("count"), eq(FieldType.INTEGER))).thenReturn(new IntegerField("count"));

        nameField = fieldFactory.createField("name", FieldType.STRING);
        countField = fieldFactory.createField("count", FieldType.INTEGER);
    }

    @Test
    public void testAppend() {

        Row rowFirst = createTestRow(1);
        Row rowSecond = createTestRow(2);
        List<RowValue> rowValues = new ArrayList<>() {{
            add(new LongRowValue(nameField.valueOf("name1"), countField.valueOf(BigInteger.valueOf(1))));
            add(new LongRowValue(nameField.valueOf("name2"), countField.valueOf(BigInteger.valueOf(2))));
        }};

        persister.append(rowFirst);
        Result actual = persister.append(rowSecond);

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(2, 2, emptyList());
        Assert.assertEquals(expected, actual);
    }

    private Row createTestRow(int number) {

        return new Row(new LinkedHashMap<>() {{
            put("name", "name" + number);
            put("count", BigInteger.valueOf(number));
        }});
    }

    @Test
    public void testProcess() {

        Row rowFirst = createTestRow(1);
        List<RowValue> rowValues = new ArrayList<>() {{
            add(new LongRowValue(nameField.valueOf("name1"), countField.valueOf(BigInteger.valueOf(1))));
        }};

        persister.append(rowFirst);
        Result actual = persister.process();

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(1, 1, emptyList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAppendWithErrors() {

        Row rowFirst = createTestRow(1);
        Row rowSecond = createTestRow(2);

        List<RowValue> rowValues = new ArrayList<>() {{
            add(new LongRowValue(nameField.valueOf("name1"), countField.valueOf(BigInteger.valueOf(1))));
            add(new LongRowValue(nameField.valueOf("name2"), countField.valueOf(BigInteger.valueOf(2))));
        }};

        String code = "row.not.unique";
        doThrow(new NotUniqueException(code)).when(draftDataService).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));

        persister.append(rowFirst);
        Result actual = persister.append(rowSecond);

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(0, 2, singletonList(new Message(code, code)));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testProcessWithErrors() {

        Row rowFirst = createTestRow(1);
        List<RowValue> rowValues = new ArrayList<>() {{
            add(new LongRowValue(nameField.valueOf("name1"), countField.valueOf(BigInteger.valueOf(1))));
        }};
        String code = "row.not.unique";
        doThrow(new NotUniqueException(code)).when(draftDataService).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));

        persister.append(rowFirst);
        try {
            persister.process();
            fail("rows are not unique");

        } catch (UserException e) {
            Assert.assertEquals(code, e.getCode());
        }
        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
    }

    public static Structure createTestStructure() {

        Structure structure = new Structure();
        structure.add(Structure.Attribute.buildPrimary("name", "name", FieldType.STRING, null), null);
        structure.add(Structure.Attribute.build("count", "count", FieldType.INTEGER, null), null);

        return structure;
    }
}
