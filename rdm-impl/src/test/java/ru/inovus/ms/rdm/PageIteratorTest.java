package ru.inovus.ms.rdm;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import ru.inovus.ms.rdm.model.AbstractCriteria;
import ru.inovus.ms.rdm.util.PageIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageIteratorTest {

    private static final List<String> allContent = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8");

    @Test
    public void testIteration() {

        Function<TestCriteria, Page<String>> pageSource = (criteria) -> {
            int total = allContent.size();
            int offset = criteria.getPageNumber() * criteria.getPageSize();
            List<String> content = allContent.subList(Math.min(offset, total), Math.min(offset + criteria.getPageSize(), total));
            return new PageImpl<>(content, criteria, total);
        };

        TestCriteria criteria = new TestCriteria();
        criteria.setPageSize(3);
        criteria.setOrders(Collections.singletonList(new Sort.Order(Sort.Direction.ASC, "id")));
        PageIterator<String, TestCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        List<List<String>> expectedPages = new ArrayList<>();
        expectedPages.add(Arrays.asList("1", "2", "3"));
        expectedPages.add(Arrays.asList("4", "5", "6"));
        expectedPages.add(Arrays.asList("7", "8"));

        for (int i = 0; i<3; i++) {
            Assert.assertTrue((i+1) + " - page number of 3 not found", pageIterator.hasNext());
            Page<String> page = pageIterator.next();
            String displayContent = String.join(",", page.getContent());
            Assert.assertTrue(displayContent + " - unexpected content", expectedPages.remove(page.getContent()));
        }
    }

    //@Test
    public void testFilteredIteration() {

        Function<TestCriteria, Page<String>> pageSource = (criteria) -> {
            int total = allContent.size();
            int offset = criteria.getPageNumber() * criteria.getPageSize();
            List<String> content = allContent.subList(Math.min(offset, total), Math.min(offset + criteria.getPageSize(), total));
            List<String> subContent = content.stream().filter(value -> Integer.parseInt(value) % 4 == 0).collect(Collectors.toUnmodifiableList());
            return new PageImpl<>(subContent, criteria, total);
        };

        TestCriteria criteria = new TestCriteria();
        criteria.setPageSize(3);
        criteria.setOrders(Collections.singletonList(new Sort.Order(Sort.Direction.ASC, "id")));
        PageIterator<String, TestCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        List<List<String>> expectedPages = new ArrayList<>();
        expectedPages.add(Collections.emptyList());
        expectedPages.add(Collections.singletonList("4"));
        expectedPages.add(Collections.singletonList("8"));

        for (int i = 0; i < 3; i++) {
            Assert.assertTrue((i+1) + " - page number of 3 not found", pageIterator.hasNext());
            Page<String> page = pageIterator.next();
            String displayContent = String.join(",", page.getContent());
            Assert.assertTrue(displayContent + " - unexpected content", expectedPages.remove(page.getContent()));
        }
    }

    private class TestCriteria extends AbstractCriteria {

    }
}
