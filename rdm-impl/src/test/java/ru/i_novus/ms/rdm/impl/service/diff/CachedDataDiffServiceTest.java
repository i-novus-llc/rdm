package ru.i_novus.ms.rdm.impl.service.diff;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookAttributeDiff;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiffCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.service.diff.VersionDataDiffService;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.DELETED;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.INSERTED;

@RunWith(MockitoJUnitRunner.class)
public class CachedDataDiffServiceTest {

    private static final int PAGE_SIZE = 3;

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String CODE = "code";

    private static final Set<List<AttributeFilter>> PRIMARY_ATTR_FILTER =
            Set.of(List.of(new AttributeFilter(ID, 1, FieldType.INTEGER)));

    private static RefBookAttributeDiff attributeDiff;

    private static List<VersionDataDiff> allVersionDataDiffs;
    private static List<VersionDataDiff> annihilatedDataDiffs;
    private static List<VersionDataDiff> annihilatedAndDeletedDataDiffs;
    private static List<VersionDataDiff> annihilatedAndInsertedDataDiffs;
    private static List<VersionDataDiff> insertedAndDeletedDataDiffs;
    private static List<VersionDataDiff> insertedDataDiffs;
    private static List<VersionDataDiff> deletedDataDiffs;

    private static CompareDataCriteria criteria14;
    private static CompareDataCriteria criteria14Inserted;
    private static CompareDataCriteria criteria14InsertedCountOnly;
    private static CompareDataCriteria criteria41;
    private static CompareDataCriteria criteria41Inserted;
    private static CompareDataCriteria criteria41InsertedCountOnly;

    @InjectMocks
    private CachedDataDiffServiceImpl cachedDataDiffService;

    @Mock
    private VersionDataDiffService versionDataDiffService;

    @Before
    public void setUp() {

        attributeDiff = new RefBookAttributeDiff(List.of(NAME), List.of(), List.of());

        VersionDataDiff versionDataDiff1 = createVersionDataDiff(ID, 1, INSERTED, INSERTED);
        VersionDataDiff versionDataDiff2 = createVersionDataDiff(ID, 2, INSERTED, DELETED);
        VersionDataDiff versionDataDiff3 = createVersionDataDiff(ID, 3, INSERTED, INSERTED);
        VersionDataDiff versionDataDiff4 = createVersionDataDiff(ID, 4, DELETED, INSERTED);
        VersionDataDiff versionDataDiff5 = createVersionDataDiff(ID, 5, DELETED, DELETED);
        VersionDataDiff versionDataDiff6 = createVersionDataDiff(ID, 6, INSERTED, INSERTED);
        VersionDataDiff versionDataDiff7 = createVersionDataDiff(ID, 7, INSERTED, INSERTED);

        insertedDataDiffs = List.of(versionDataDiff1, versionDataDiff3, versionDataDiff6, versionDataDiff7);
        deletedDataDiffs = singletonList(versionDataDiff5);
        annihilatedDataDiffs = List.of(versionDataDiff2, versionDataDiff4);

        annihilatedAndDeletedDataDiffs = new ArrayList<>();
        annihilatedAndDeletedDataDiffs.addAll(annihilatedDataDiffs);
        annihilatedAndDeletedDataDiffs.addAll(deletedDataDiffs);
        annihilatedAndDeletedDataDiffs.sort(comparing(VersionDataDiff::getPrimaryValues));

        annihilatedAndInsertedDataDiffs = new ArrayList<>();
        annihilatedAndInsertedDataDiffs.addAll(annihilatedDataDiffs);
        annihilatedAndInsertedDataDiffs.addAll(insertedDataDiffs);
        annihilatedAndInsertedDataDiffs.sort(comparing(VersionDataDiff::getPrimaryValues));

        insertedAndDeletedDataDiffs = new ArrayList<>();
        insertedAndDeletedDataDiffs.addAll(insertedDataDiffs);
        insertedAndDeletedDataDiffs.addAll(deletedDataDiffs);
        insertedAndDeletedDataDiffs.sort(comparing(VersionDataDiff::getPrimaryValues));

        allVersionDataDiffs = new ArrayList<>();
        allVersionDataDiffs.addAll(insertedDataDiffs);
        allVersionDataDiffs.addAll(deletedDataDiffs);
        allVersionDataDiffs.addAll(annihilatedDataDiffs);
        allVersionDataDiffs.sort(comparing(VersionDataDiff::getPrimaryValues));

        criteria14 = createCriteria(1, 4, null, null);
        criteria14Inserted = createCriteria(1, 4, INSERTED, null);
        criteria14InsertedCountOnly = createCriteria(1, 4, INSERTED, true);

        criteria41 = createCriteria(4, 1, null, null);
        criteria41Inserted = createCriteria(4, 1, INSERTED, null);
        criteria41InsertedCountOnly = createCriteria(4, 1, INSERTED, true);
    }

    private void mockRequestsForExcluded(CompareDataCriteria criteria) {
        VersionDataDiffCriteria excludedSearchCriteriaPage1 = toExcludedCriteria(criteria);
        VersionDataDiffCriteria excludedSearchCriteriaPage2 = createCriteriaWithPageNumber(excludedSearchCriteriaPage1, 1);
        VersionDataDiffCriteria excludedSearchCriteriaPage3 = createCriteriaWithPageNumber(excludedSearchCriteriaPage1, 2);
        VersionDataDiffCriteria excludedSearchCriteriaPage4 = createCriteriaWithPageNumber(excludedSearchCriteriaPage1, 3);

        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage1))).thenReturn(createPage(allVersionDataDiffs, 0));
        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage2))).thenReturn(createPage(allVersionDataDiffs, 1));
        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage3))).thenReturn(createPage(allVersionDataDiffs, 2));
        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage4))).thenReturn(Page.empty());
    }

    @Test
    public void testGetCachedDataDifference_sameOldAndNewVersion() {
        CompareDataCriteria criteria = new CompareDataCriteria(1, 1);

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, 0, 0);

        verify(versionDataDiffService, times(0)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(0)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_noCachedDiffs() {
        CompareDataCriteria criteria = new CompareDataCriteria(3, 5);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(false);
        when(versionDataDiffService.search(eq(toExcludedCriteria(criteria)))).thenThrow(NotFoundException.class);

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertNull(dataDifference);

        verify(versionDataDiffService, times(1)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(1)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_noDataChanges() {
        CompareDataCriteria criteria = new CompareDataCriteria(3, 5);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(false);
        when(versionDataDiffService.search(eq(toExcludedCriteria(criteria)))).thenReturn(Page.empty());

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, 0, 0);

        verify(versionDataDiffService, times(1)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(2)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_noDataChanges_backward() {
        CompareDataCriteria criteria = new CompareDataCriteria(3, 5);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(true);
        when(versionDataDiffService.search(eq(toExcludedCriteria(criteria)))).thenReturn(Page.empty());

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, 0, 0);

        verify(versionDataDiffService, times(1)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(2)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_severalPages() {

        CompareDataCriteria criteria = criteria14;
        mockRequestsForExcluded(criteria);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(false);

        VersionDataDiffCriteria excludedSearchCriteriaPage1 = toExcludedCriteria(criteria);
        VersionDataDiffCriteria excludedSearchCriteriaPage2 = createCriteriaWithPageNumber(excludedSearchCriteriaPage1, 1);
        VersionDataDiffCriteria excludedSearchCriteriaPage3 = createCriteriaWithPageNumber(excludedSearchCriteriaPage1, 2);
        VersionDataDiffCriteria excludedSearchCriteriaPage4 = createCriteriaWithPageNumber(excludedSearchCriteriaPage1, 3);

        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage1))).thenReturn(createPage(allVersionDataDiffs, 0));
        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage2))).thenReturn(createPage(allVersionDataDiffs, 1));
        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage3))).thenReturn(createPage(allVersionDataDiffs, 2));
        when(versionDataDiffService.search(eq(excludedSearchCriteriaPage4))).thenReturn(Page.empty());

        List<String> excludedPrimaryValues = annihilatedDataDiffs.stream().map(VersionDataDiff::getPrimaryValues).collect(Collectors.toList());
        VersionDataDiffCriteria criteriaWithExcludedListPage1 = new VersionDataDiffCriteria(criteria, excludedPrimaryValues);
        VersionDataDiffCriteria criteriaWithExcludedListPage2 = createCriteriaWithPageNumber(criteriaWithExcludedListPage1, 1);
        VersionDataDiffCriteria criteriaWithExcludedListPage3 = createCriteriaWithPageNumber(criteriaWithExcludedListPage1, 2);

        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage1))).thenReturn(createPage(insertedAndDeletedDataDiffs, 0));
        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage2))).thenReturn(createPage(insertedAndDeletedDataDiffs, 1));
        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage3))).thenReturn(createPageWithEmptyContent(insertedAndDeletedDataDiffs.size()));

        int expectedCount = insertedAndDeletedDataDiffs.size();

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 3);

        criteria.setPageNumber(criteria.getPageNumber() + 1);
        dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 2);

        criteria.setPageNumber(criteria.getPageNumber() + 1);
        dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 0);

        verify(versionDataDiffService, times(3)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(15)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_severalPages_backward() {
        CompareDataCriteria criteria = criteria41;
        mockRequestsForExcluded(criteria);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(true);

        List<String> excludedPrimaryValues = annihilatedDataDiffs.stream().map(VersionDataDiff::getPrimaryValues).collect(Collectors.toList());
        VersionDataDiffCriteria criteriaWithExcludedListPage1 = new VersionDataDiffCriteria(criteria, excludedPrimaryValues);
        VersionDataDiffCriteria criteriaWithExcludedListPage2 = createCriteriaWithPageNumber(criteriaWithExcludedListPage1, 1);
        VersionDataDiffCriteria criteriaWithExcludedListPage3 = createCriteriaWithPageNumber(criteriaWithExcludedListPage1, 2);

        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage1))).thenReturn(createPage(insertedAndDeletedDataDiffs, 0));
        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage2))).thenReturn(createPage(insertedAndDeletedDataDiffs, 1));
        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage3))).thenReturn(createPageWithEmptyContent(insertedAndDeletedDataDiffs.size()));

        int expectedCount = insertedAndDeletedDataDiffs.size();

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 3);

        criteria.setPageNumber(criteria.getPageNumber() + 1);
        dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 2);

        criteria.setPageNumber(criteria.getPageNumber() + 1);
        dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 0);

        verify(versionDataDiffService, times(3)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(15)).search(any());
    }


    @Test
    public void testGetCachedDataDifference_severalPages_filterByStatus() {

        CompareDataCriteria criteria = criteria14Inserted;
        mockRequestsForExcluded(criteria);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(false);

        List<String> excludedPrimaryValues = annihilatedAndDeletedDataDiffs.stream().map(VersionDataDiff::getPrimaryValues).collect(Collectors.toList());
        VersionDataDiffCriteria criteriaWithExcludedListPage1 = new VersionDataDiffCriteria(criteria, excludedPrimaryValues);
        VersionDataDiffCriteria criteriaWithExcludedListPage2 = createCriteriaWithPageNumber(criteriaWithExcludedListPage1, 1);
        VersionDataDiffCriteria criteriaWithExcludedListPage3 = createCriteriaWithPageNumber(criteriaWithExcludedListPage1, 2);

        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage1))).thenReturn(createPage(insertedDataDiffs, 0));
        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage2))).thenReturn(createPage(insertedDataDiffs, 1));
        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage3))).thenReturn(createPageWithEmptyContent(insertedDataDiffs.size()));

        int expectedCount = insertedDataDiffs.size();

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 3);

        criteria.setPageNumber(criteria.getPageNumber() + 1);
        dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 1);

        criteria.setPageNumber(criteria.getPageNumber() + 1);
        dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 0);

        verify(versionDataDiffService, times(3)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(15)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_severalPages_filterByStatus_backward() {

        CompareDataCriteria criteria = criteria41Inserted;
        mockRequestsForExcluded(criteria);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(true);

        List<String> excludedPrimaryValues = annihilatedAndInsertedDataDiffs.stream().map(VersionDataDiff::getPrimaryValues).collect(Collectors.toList());
        VersionDataDiffCriteria criteriaWithExcludedListPage1 = new VersionDataDiffCriteria(criteria, excludedPrimaryValues);
        VersionDataDiffCriteria criteriaWithExcludedListPage2 = createCriteriaWithPageNumber(criteriaWithExcludedListPage1, 1);

        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage1))).thenReturn(createPage(deletedDataDiffs, 0));
        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage2))).thenReturn(createPageWithEmptyContent(deletedDataDiffs.size()));

        int expectedCount = deletedDataDiffs.size();

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 1);

        criteria.setPageNumber(criteria.getPageNumber() + 1);
        dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 0);


        verify(versionDataDiffService, times(2)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(10)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_severalPages_filterByStatus_countOnly() {

        CompareDataCriteria criteria = criteria14InsertedCountOnly;
        mockRequestsForExcluded(criteria);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(false);

        List<String> excludedPrimaryValues = annihilatedAndDeletedDataDiffs.stream().map(VersionDataDiff::getPrimaryValues).collect(Collectors.toList());
        VersionDataDiffCriteria criteriaWithExcludedListPage1 = new VersionDataDiffCriteria(criteria, excludedPrimaryValues);

        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage1))).thenReturn(createPage(insertedDataDiffs, 0));

        int expectedCount = insertedDataDiffs.size();

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);

        assertDataDifference(dataDifference, expectedCount, 0);

        verify(versionDataDiffService, times(1)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(5)).search(any());
    }

    @Test
    public void testGetCachedDataDifference_severalPages_filterByStatus_countOnly_backward() {

        CompareDataCriteria criteria = criteria41InsertedCountOnly;
        mockRequestsForExcluded(criteria);

        when(versionDataDiffService.isPublishedBefore(eq(criteria.getNewVersionId()), eq(criteria.getOldVersionId()))).thenReturn(true);

        List<String> excludedPrimaryValues = annihilatedAndInsertedDataDiffs.stream().map(VersionDataDiff::getPrimaryValues).collect(Collectors.toList());
        VersionDataDiffCriteria criteriaWithExcludedListPage1 = new VersionDataDiffCriteria(criteria, excludedPrimaryValues);

        when(versionDataDiffService.search(eq(criteriaWithExcludedListPage1))).thenReturn(createPage(deletedDataDiffs, 0));

        int expectedCount = deletedDataDiffs.size();

        DataDifference dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        assertDataDifference(dataDifference, expectedCount, 0);

        verify(versionDataDiffService, times(1)).isPublishedBefore(any(), any());
        verify(versionDataDiffService, times(5)).search(any());
    }

    private void assertDataDifference(DataDifference dataDifference, int expectedTotalElements, int expectedPageContentSize) {
        assertEquals(expectedTotalElements, dataDifference.getRows().getCount());
        assertEquals(expectedPageContentSize, dataDifference.getRows().getCollection().size());
    }

    private VersionDataDiffCriteria toExcludedCriteria(CompareDataCriteria criteria) {
        VersionDataDiffCriteria versionDataDiffCriteria = new VersionDataDiffCriteria();
        versionDataDiffCriteria.setOldVersionId(criteria.getOldVersionId());
        versionDataDiffCriteria.setNewVersionId(criteria.getNewVersionId());
        versionDataDiffCriteria.setPrimaryAttributesFilters(criteria.getPrimaryAttributesFilters());
        return versionDataDiffCriteria;
    }

    private static VersionDataDiffCriteria createCriteriaWithPageNumber(VersionDataDiffCriteria src, int pageNumber) {
        VersionDataDiffCriteria criteria = new VersionDataDiffCriteria(src);
        criteria.setPageNumber(pageNumber);
        return criteria;
    }

    private DiffRowValue createInserted(Integer pkValue) {
        return createDiffRowValue(pkValue, INSERTED);
    }

    private DiffRowValue createDeleted(Integer pkValue) {
        return createDiffRowValue(pkValue, DELETED);
    }

    private DiffRowValue createDiffRowValue(Integer pkValue, DiffStatusEnum status) {
        return new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), status == INSERTED ? null : pkValue, status == INSERTED ? pkValue : null, status),
                        new DiffFieldValue<>(new StringField(CODE), status == INSERTED ? null : "test_" + CODE, status == INSERTED ? "test_" + CODE : null, status),
                        new DiffFieldValue<>(new StringField(NAME), status == INSERTED ? null : "test_" + NAME, status == INSERTED ? "test_" + NAME : null, status)
                ),
                status);
    }

    private Page<VersionDataDiff> createPage(List<VersionDataDiff> versionDataDiffs, int pageNumber) {
        int toIndex = PAGE_SIZE * (pageNumber + 1);
        return new PageImpl<>(
                versionDataDiffs.subList(PAGE_SIZE * pageNumber, toIndex > versionDataDiffs.size() ? versionDataDiffs.size() : toIndex),
                PageRequest.of(pageNumber, PAGE_SIZE), versionDataDiffs.size());
    }

    private PageImpl<VersionDataDiff> createPageWithEmptyContent(int totalElements) {
        return new PageImpl<>(emptyList(), PageRequest.of(1, PAGE_SIZE), totalElements);
    }

    private VersionDataDiff createVersionDataDiff(String pkName, int pkValue, DiffStatusEnum firstRowValuesStatus, DiffStatusEnum lastRowValueStatus) {
        return new VersionDataDiff(pkName + "=" + pkValue,
                createDiffRowValue(pkValue, firstRowValuesStatus),
                createDiffRowValue(pkValue, lastRowValueStatus));
    }

    private CompareDataCriteria createCriteria(Integer oldVersionId, Integer newVersionId, DiffStatusEnum status, Boolean countOnly) {
        CompareDataCriteria criteria = new CompareDataCriteria(oldVersionId, newVersionId);
        criteria.setDiffStatus(status);
        criteria.setCountOnly(countOnly);
        criteria.setPrimaryAttributesFilters(PRIMARY_ATTR_FILTER);
        criteria.setPageSize(PAGE_SIZE);
        return criteria;
    }
}