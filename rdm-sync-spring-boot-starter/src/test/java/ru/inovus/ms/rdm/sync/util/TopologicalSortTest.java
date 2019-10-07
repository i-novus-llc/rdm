package ru.inovus.ms.rdm.sync.util;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@RunWith(JUnit4.class)
public class TopologicalSortTest {

    @Test
    public void testGetOrder() {
//      Описываем структуру ссылок в справочниках:
//          5 -----> 2
//        /   \       \
//      7      4       1 -----> 0
//        \   /       /
//          6 -----> 3
//
//      Все ссылки идут слева направо.
//      Более формально:
//      7 -> 5, 7 -> 6;
//      5 -> 4, 5 -> 2
//      6 -> 4, 6 -> 3
//      2 -> 1
//      3 -> 1
//      1 -> 0
        RefBook
            v0 = new RefBook(),
            v1 = new RefBook(),
            v2 = new RefBook(),
            v3 = new RefBook(),
            v4 = new RefBook(),
            v5 = new RefBook(),
            v6 = new RefBook(),
            v7 = new RefBook();
        v0.setCode("0");
        v1.setCode("1");
        v2.setCode("2");
        v3.setCode("3");
        v4.setCode("4");
        v5.setCode("5");
        v6.setCode("6");
        v7.setCode("7");
        v0.setStructure(new Structure(emptyList(), emptyList()));
        v1.setStructure(new Structure(emptyList(), singletonList(new Structure.Reference("", v0.getCode(), ""))));
        v2.setStructure(new Structure(emptyList(), singletonList(new Structure.Reference("", v1.getCode(), ""))));
        v3.setStructure(new Structure(emptyList(), singletonList(new Structure.Reference("", v1.getCode(), ""))));
        v4.setStructure(new Structure(emptyList(), emptyList()));
        v5.setStructure(new Structure(emptyList(), List.of(new Structure.Reference("", v2.getCode(), ""), new Structure.Reference("", v4.getCode(), ""))));
        v6.setStructure(new Structure(emptyList(), List.of(new Structure.Reference("", v3.getCode(), ""), new Structure.Reference("", v4.getCode(), ""))));
        v7.setStructure(new Structure(emptyList(), List.of(new Structure.Reference("", v5.getCode(), ""), new Structure.Reference("", v6.getCode(), ""))));
        List<RefBook> refBooks = new java.util.ArrayList<>(List.of(v0, v1, v2, v3, v4, v5, v6, v7));
        Collections.shuffle(refBooks); // Перемешиваем, чтобы рандомизировать все
        List<String> inverseOrder = TopologicalSort.getInverseOrder(refBooks);
        Assert.assertThat(inverseOrder, CoreMatchers.is(Arrays.asList(v0.getCode(), v1.getCode(), v2.getCode(), v3.getCode(), v4.getCode(), v5.getCode(), v6.getCode(), v7.getCode())));
    }

}
