package ru.inovus.ms.rdm.api.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.stream;

@RunWith(JUnit4.class)
public class PaginateTest {

    private static final Integer[] INTS = {1, 9, 2, 5, 3, 6, 4, 7, 8, 8, 1, 8, 9, 1};
    private static final Function<? super GetIntCriteria, Page<? extends Integer>> PAGE_SUPPLY = criteria -> {
        Integer[] integers = criteria.intIn.length == 0 ? INTS : stream(INTS).filter(i -> stream(criteria.intIn).anyMatch(j -> j == i)).toArray(Integer[]::new);
        int total = integers.length;
        int from = criteria.getPageNumber() * criteria.getPageSize();
        if (from >= integers.length)
            return Page.empty();
        int to = from + criteria.getPageSize();
        List<Integer> result = new ArrayList<>();
        for (int i = from; i < to && i < integers.length; i++) {
            result.add(integers[i]);
        }
        return new PageImpl<>(result, Pageable.unpaged(), total);
    };

    @Test
    public void testFindOneSuchThat() {
        Paginate.<GetIntCriteria, Integer>over(new GetIntCriteria(new int[] {})).
                withPageSupply(PAGE_SUPPLY).
                pageSize(3).
                defaultSortProvided().
                findOneSuchThat(i -> i == 9).
                ifPresentOrElse(i -> {}, Assert::fail);
        Paginate.<GetIntCriteria, Integer>over(new GetIntCriteria(new int[] {})).
                withPageSupply(PAGE_SUPPLY).
                pageSize(3).
                defaultSortProvided().
                findOneSuchThat(i -> i == 666).
                ifPresentOrElse(i -> Assert.fail(), () -> {});
    }

    @Test
    public void testForEachWithVariousPageSizes() {
        for (int i = 1; i < 100; i++) {
            testForEach(i);
        }
    }

    public void testForEach(int pageSize) {
        final int[] numMatched = {0};
        Paginate.<GetIntCriteria, Integer>over(new GetIntCriteria(new int[] {1, 2, 3})).
                withPageSupply(PAGE_SUPPLY).
                pageSize(pageSize).
                defaultSortProvided().forEach(i -> numMatched[0]++);
        Assert.assertEquals(5, numMatched[0]);
    }

    private static class GetIntCriteria extends AbstractCriteria {
        private int[] intIn;
        GetIntCriteria(int[] intIn) {this.intIn = intIn;}
    }

}
