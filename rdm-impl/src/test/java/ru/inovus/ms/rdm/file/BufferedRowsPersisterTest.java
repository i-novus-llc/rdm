package ru.inovus.ms.rdm.file;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Structure;

import java.util.*;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BufferedRowsPersisterTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";

    private static final int BUFFER_SIZE = 2;

    private Field name;

    private Field count;

    private BufferedRowsPersister bufferedRowsPersister;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private DraftDataService draftDataService;

    @Before
    public void setUp() {
        bufferedRowsPersister = new BufferedRowsPersister(BUFFER_SIZE, draftDataService, TEST_STORAGE_CODE, createTestStructure());
        when(fieldFactory.createField(eq("name"), eq(FieldType.STRING))).thenReturn(new StringField("name"));
        when(fieldFactory.createField(eq("count"), eq(FieldType.INTEGER))).thenReturn(new IntegerField("count"));
        name = fieldFactory.createField("name", FieldType.STRING);
        count = fieldFactory.createField("count", FieldType.INTEGER);
    }

    @Test
    public void testAppend() {
        Row rowFirst = createTestRow(1);
        Row rowSecond = createTestRow(2);
        List<RowValue> rowValues = new ArrayList() {{
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(1)));
            add(new LongRowValue(name.valueOf("name2"), count.valueOf(2)));
        }};

        bufferedRowsPersister.append(rowFirst);
        Result actual = bufferedRowsPersister.append(rowSecond);

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(2, 2, null);
        Assert.assertEquals(expected, actual);
    }

    private Row createTestRow(int number) {
        return new Row(new LinkedHashMap() {{
            put("name", "name" + number);
            put("count", number);
        }});
    }

    @Test
    public void testProcess() {
        Row rowFirst = createTestRow(1);
        List<RowValue> rowValues = new ArrayList() {{
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(1)));
        }};

        bufferedRowsPersister.append(rowFirst);
        Result actual = bufferedRowsPersister.process();

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(1, 1, null);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAppendWithErrors() {
        Row rowFirst = createTestRow(1);
        Row rowSecond = createTestRow(2);
        List<RowValue> rowValues = new ArrayList() {{
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(1)));
            add(new LongRowValue(name.valueOf("name2"), count.valueOf(2)));
        }};
        String message = "something wrong";
        doThrow(new RuntimeException(message)).when(draftDataService).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));

        bufferedRowsPersister.append(rowFirst);
        Result actual = bufferedRowsPersister.append(rowSecond);

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(0, 2, Collections.singletonList(message));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testProcessWithErrors() {
        Row rowFirst = createTestRow(1);
        List<RowValue> rowValues = new ArrayList() {{
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(1)));
        }};
        String message = "something wrong";
        doThrow(new RuntimeException(message)).when(draftDataService).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));

        bufferedRowsPersister.append(rowFirst);
        Result actual = bufferedRowsPersister.process();

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(0, 1, Collections.singletonList(message));
        Assert.assertEquals(expected, actual);
    }

    public static Structure createTestStructure() {
        Structure structure = new Structure();
        structure.setAttributes(new LinkedList() {{
            add(Structure.Attribute.build("name", "name", FieldType.STRING, false, "description"));
            add(Structure.Attribute.build("count", "count", FieldType.INTEGER, false, "description"));
        }});
        return structure;
    }
}
