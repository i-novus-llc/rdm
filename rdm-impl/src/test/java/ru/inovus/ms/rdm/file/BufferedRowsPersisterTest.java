package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;
import ru.inovus.ms.rdm.file.process.BufferedRowsPersister;
import ru.inovus.ms.rdm.n2o.model.Result;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.Structure;

import java.math.BigInteger;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        List<RowValue> rowValues = new ArrayList<>() {{
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(BigInteger.valueOf(1))));
            add(new LongRowValue(name.valueOf("name2"), count.valueOf(BigInteger.valueOf(2))));
        }};

        bufferedRowsPersister.append(rowFirst);
        Result actual = bufferedRowsPersister.append(rowSecond);

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
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(BigInteger.valueOf(1))));
        }};

        bufferedRowsPersister.append(rowFirst);
        Result actual = bufferedRowsPersister.process();

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(1, 1, emptyList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAppendWithErrors() {
        Row rowFirst = createTestRow(1);
        Row rowSecond = createTestRow(2);
        List<RowValue> rowValues = new ArrayList<>() {{
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(BigInteger.valueOf(1))));
            add(new LongRowValue(name.valueOf("name2"), count.valueOf(BigInteger.valueOf(2))));
        }};
        String code = "row.not.unique";
        doThrow(new NotUniqueException(code)).when(draftDataService).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));

        bufferedRowsPersister.append(rowFirst);
        Result actual = bufferedRowsPersister.append(rowSecond);

        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
        Result expected = new Result(0, 2, singletonList(new Message(code, code)));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testProcessWithErrors() {
        Row rowFirst = createTestRow(1);
        List<RowValue> rowValues = new ArrayList<>() {{
            add(new LongRowValue(name.valueOf("name1"), count.valueOf(BigInteger.valueOf(1))));
        }};
        String code = "row.not.unique";
        doThrow(new NotUniqueException(code)).when(draftDataService).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));

        bufferedRowsPersister.append(rowFirst);
        try {
            bufferedRowsPersister.process();
            fail("rows are not unique");
        } catch (UserException e) {
            Assert.assertEquals(code, e.getCode());
        }
        verify(draftDataService, times(1)).addRows(eq(TEST_STORAGE_CODE), eq(rowValues));
    }

    public static Structure createTestStructure() {
        Structure structure = new Structure();
        structure.setAttributes(new LinkedList<>() {{
            add(Structure.Attribute.buildPrimary("name", "name", FieldType.STRING, "description"));
            add(Structure.Attribute.build("count", "count", FieldType.INTEGER, "description"));
        }});
        return structure;
    }
}
