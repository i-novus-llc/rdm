package ru.i_novus.ms.rdm.sync.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class RefBookReferenceSortTest {

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
        for (int i = 0; i < 1000; i++) {
            Collections.shuffle(refBooks); // Перемешиваем, чтобы рандомизировать все
            List<String> inverseOrder = RefBookReferenceSort.getSortedCodes(refBooks);
            testOrder(inverseOrder, refBooks);
        }
    }

    private void testOrder(List<String> inverseOrder, List<RefBook> refBooks) {
        Set<String> visited = new HashSet<>();
        Map<String, RefBook> m = refBooks.stream().map(RefBook::new).collect(toMap(RefBookVersion::getCode, identity()));
        for (String s : inverseOrder) {
            RefBook refBook = m.get(s);
//          Если здесь true -- значит у нас либо неправильная топологическая сортировка,
//          либо справочники содержат циклические ссылки
            assertThat(visited.contains(refBook.getCode()), is(false));
            visited.add(refBook.getCode());
            for (Structure.Reference ref : refBook.getStructure().getReferences()) {
//              Если здесь false -- значит сортировка неправильная, потому что
//              мы не посетили вершину, на которую ссылаемся
                assertThat(visited.contains(ref.getReferenceCode()), is(true));
            }
        }
    }

}
