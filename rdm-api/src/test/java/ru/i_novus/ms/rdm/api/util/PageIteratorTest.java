package ru.i_novus.ms.rdm.api.util;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import java.util.*;

public class PageIteratorTest {

    private static final List<String> allContent = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8");

    @Test
    public void testIteration() {

        TestCriteria criteria = new TestCriteria();
        criteria.setPageSize(3);
        criteria.setOrders(Collections.singletonList(new Sort.Order(Sort.Direction.ASC, "id")));

        PageIterator<String, TestCriteria> pageIterator = new PageIterator<>(c -> {
            int total = allContent.size();
            int offset = c.getPageNumber() * c.getPageSize();
            List<String> content = allContent.subList(Math.min(offset, total), Math.min(offset + c.getPageSize(), total));
            return new PageImpl<>(content, c, total);
        }, criteria);

        List<List<String>> expectedPages = new ArrayList<>();
        expectedPages.add(Arrays.asList("1", "2", "3"));
        expectedPages.add(Arrays.asList("4", "5", "6"));
        expectedPages.add(Arrays.asList("7", "8"));

        for (int i = 0; i<3; i++) {
            Assert.assertTrue((i+1) + " - page number of 3 not found", pageIterator.hasNext());
            Page<? extends String> page = pageIterator.next();
            String displayContent = String.join(",", page.getContent());
            Assert.assertTrue(displayContent + " - unexpected content", expectedPages.remove(page.getContent()));
        }
    }

    private static class TestCriteria extends AbstractCriteria {

    }
}
