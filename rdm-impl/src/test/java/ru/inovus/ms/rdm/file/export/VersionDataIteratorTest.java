package ru.inovus.ms.rdm.file.export;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.n2o.service.VersionServiceImpl;
import ru.inovus.ms.rdm.n2o.util.ConverterUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

/**
 * Created by znurgaliev on 24.07.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class VersionDataIteratorTest {

    @Mock
    private VersionServiceImpl versionService;

    @Test
    public void testVersionDataIterator() {

        //Создаем коллекции строк, для 2х версий (тестоваые данные)
        List<RowValue> firstVersionRows = new ArrayList<>();
        for (int i = 0; i < 50; i++){
            LongRowValue rowValue = new LongRowValue();
            rowValue.setFieldValues(Collections.singletonList(new IntegerFieldValue("integerField1", i)));
            firstVersionRows.add(rowValue);
        }
        List<RowValue> secondVersionRows = new ArrayList<>();
        for (int i = 0; i < 1001; i++){
            LongRowValue rowValue = new LongRowValue();
            rowValue.setFieldValues(Collections.singletonList(new IntegerFieldValue("integerField2", i)));
            secondVersionRows.add(rowValue);
        }

        //в зависимости от параметров, возвращаем разные строки из тестовых данных
        setReturnVersionRowsOnMockInvocation(1, firstVersionRows);
        setReturnVersionRowsOnMockInvocation(2, secondVersionRows);

        List<RowValue> allRows = new ArrayList<>();
        allRows.addAll(firstVersionRows);
        allRows.addAll(secondVersionRows);
        List<Row> expectedRows = allRows.stream().map(ConverterUtil::toRow).collect(Collectors.toList());

        //вызов hasNext и next
        VersionDataIterator dataIterator = new VersionDataIterator(versionService, Arrays.asList(1, 2));
        expectedRows.forEach(row -> {
                    Assert.assertTrue(dataIterator.hasNext());
                    Assert.assertEquals(row, dataIterator.next());
                });
        Assert.assertFalse(dataIterator.hasNext());

        //вызов next
        VersionDataIterator dataIterator2 = new VersionDataIterator(versionService, Arrays.asList(1, 2));
        expectedRows.forEach(row -> Assert.assertEquals(row, dataIterator2.next()));
        Assert.assertFalse(dataIterator.hasNext());

    }

    private void setReturnVersionRowsOnMockInvocation(int versionId, List<RowValue> versionRows) {
        when(versionService.search(eq(versionId), any())).then(invocationOnMock -> {
            SearchDataCriteria criteria = (SearchDataCriteria) invocationOnMock.getArguments()[1];
            int fromIndex = (int) criteria.getOffset();
            int toIndex = fromIndex + criteria.getPageSize();
            toIndex = toIndex > versionRows.size() ? versionRows.size() : toIndex;
            if (fromIndex < 0 || fromIndex >= toIndex)
                return null;
            List<RowValue> currentPage1 = versionRows.subList(fromIndex, toIndex);
            return new PageImpl<>(currentPage1, PageRequest.of(criteria.getPageNumber(), criteria.getPageSize()), versionRows.size());
        });
    }

}
