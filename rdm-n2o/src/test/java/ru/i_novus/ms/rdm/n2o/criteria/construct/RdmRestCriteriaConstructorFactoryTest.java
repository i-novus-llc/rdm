package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.criteria.api.SortingDirectionEnum;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class RdmRestCriteriaConstructorFactoryTest {

    private static final String SORTED_FIELD = "id";

    @Test
    public void testConstruct() {

        final BaseRestCriteriaConstructor baseConstructor = new BaseRestCriteriaConstructor();
        final RdmCriteriaConstructorFactory constructorFactory = new RdmCriteriaConstructorFactory(singletonList(baseConstructor));

        final N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        final AbstractCriteria abstractCriteria = new AbstractCriteria();

        final AbstractCriteria result = constructorFactory.construct(criteria, abstractCriteria);
        assertNotNull(result);
        assertNotEquals(criteria.getPage(), result.getPageNumber());
        assertEquals(criteria.getSize(), result.getPageSize());
    }

    @Test
    public void testConstruct_VersionCriteria() {

        final BaseRestCriteriaConstructor baseConstructor = new BaseRestCriteriaConstructor();
        final RdmCriteriaConstructorFactory constructorFactory = new RdmCriteriaConstructorFactory(singletonList(baseConstructor));

        final N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        final VersionCriteria versionCriteria = new VersionCriteria();

        final VersionCriteria result = constructorFactory.construct(criteria, versionCriteria);
        assertCriteriaEquals(criteria, result);
    }

    @Test
    public void testConstruct_RefBookCriteria_SortById() {

        final BaseRestCriteriaConstructor baseConstructor = new BaseRestCriteriaConstructor();
        final RefBookCriteriaConstructor refBookCriteriaConstructor = new RefBookCriteriaConstructor();
        final RdmCriteriaConstructorFactory constructorFactory = new RdmCriteriaConstructorFactory(
                List.of(baseConstructor, refBookCriteriaConstructor)
        );

        final N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        final Sorting sorting = criteria.getSorting();
        sorting.setField(SORTED_FIELD);
        sorting.setDirection(SortingDirectionEnum.ASC);
        final RefBookCriteria refBookCriteria = new RefBookCriteria();

        final RefBookCriteria result = constructorFactory.construct(criteria, refBookCriteria);
        assertNotNull(result);
        assertCriteriaEquals(criteria, result);
    }

    @Test
    public void testConstruct_RefBookCriteria_SortByName() {

        final BaseRestCriteriaConstructor baseConstructor = new BaseRestCriteriaConstructor();
        final RefBookCriteriaConstructor refBookCriteriaConstructor = new RefBookCriteriaConstructor();
        final RdmCriteriaConstructorFactory constructorFactory = new RdmCriteriaConstructorFactory(
                List.of(baseConstructor, refBookCriteriaConstructor)
        );

        final N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        final Sorting sorting = criteria.getSorting();
        sorting.setField("name");
        sorting.setDirection(SortingDirectionEnum.DESC);
        final RefBookCriteria refBookCriteria = new RefBookCriteria();

        final RefBookCriteria result = constructorFactory.construct(criteria, refBookCriteria);
        assertNotNull(result);

        final Sort sort = result.getSort();
        assertNotNull(sort);
        final Sort.Order sortOrder = sort.getOrderFor("passport.name");
        assertNotNull(sortOrder);
        assertEquals(Sort.Direction.DESC, sortOrder.getDirection());
    }

    private void assertCriteriaEquals(Criteria expected, RestCriteria actual) {

        assertNotNull(actual);
        assertEquals(expected.getPage() - 1, actual.getPageNumber());
        assertEquals(expected.getSize(), actual.getPageSize());

        assertNotNull(actual.getSort());
        assertSortingEquals(expected.getSorting(), actual.getSort().getOrderFor(SORTED_FIELD));
    }

    private void assertSortingEquals(Sorting expected, Sort.Order actual) {

        assertNotNull(actual);
        assertEquals(expected.getField(), actual.getProperty());
        assertEquals(expected.getDirection().toString(), actual.getDirection().toString());
    }

    private N2oPreparedCriteria createN2oPreparedCriteria() {

        N2oPreparedCriteria criteria = new N2oPreparedCriteria();
        criteria.setPage(11);
        criteria.setSize(111);
        criteria.addSorting(new Sorting(SORTED_FIELD, SortingDirectionEnum.ASC));

        return criteria;
    }
}