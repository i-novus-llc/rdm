package ru.i_novus.ms.rdm.impl.service.diff;

import net.n2oapp.criteria.api.CollectionPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookAttributeDiff;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiffCriteria;
import ru.i_novus.ms.rdm.api.service.diff.VersionDataDiffService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toCriteria;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.EXACT;

@Service
public class CachedDataDiffServiceImpl implements CachedDataDiffService {

    private static final Logger logger = LoggerFactory.getLogger(CachedDataDiffServiceImpl.class);

    private final VersionDataDiffService versionDataDiffService;

    @Autowired
    public CachedDataDiffServiceImpl(VersionDataDiffService versionDataDiffService) {
        this.versionDataDiffService = versionDataDiffService;
    }

    @Override
    public DataDifference getCachedDataDifference(CompareDataCriteria criteria, RefBookAttributeDiff attributeDiff) {

        if (criteria.getOldVersionId().equals(criteria.getNewVersionId()))
            return buildDataDifference(emptyList(), criteria, 0);

        if (hasIncompatibleWithCacheFilter(criteria))
            return null;

        try {
            boolean isBackwardComparison = versionDataDiffService.isPublishedBefore(criteria.getNewVersionId(), criteria.getOldVersionId());
            Set<String> changedAttributeNames = getAttributeDiffFieldNames(attributeDiff);
            List<String> excludedPkValues = getExcludedPrimaryValues(criteria, changedAttributeNames, isBackwardComparison);
            VersionDataDiffCriteria versionDataDiffCriteria = new VersionDataDiffCriteria(criteria, excludedPkValues.isEmpty() ? null : excludedPkValues);

            Page<VersionDataDiff> versionDataDiffPage = versionDataDiffService.search(versionDataDiffCriteria);
            List<DiffRowValue> pageContent = getPageContent(versionDataDiffPage.getContent(), criteria.getCountOnly(), changedAttributeNames, isBackwardComparison);

            return buildDataDifference(pageContent, criteria, Math.toIntExact(versionDataDiffPage.getTotalElements()));

        } catch (NotFoundException e) {
            logger.debug("Cached data difference not found. newVersionId={}, oldVersionId={}",
                    criteria.getNewVersionId(), criteria.getOldVersionId());
            return null;
        }
    }

    private boolean hasIncompatibleWithCacheFilter(CompareDataCriteria criteria) {
        if (isNull(criteria.getPrimaryAttributesFilters()))
            return false;

        return criteria.getPrimaryAttributesFilters().stream()
                .anyMatch(filters -> filters.stream()
                        .anyMatch(filter -> EXACT != filter.getSearchType()));
    }

    private DataDifference buildDataDifference(List<DiffRowValue> pageContent, CompareDataCriteria criteria, int totalElements) {
        return new DataDifference(new CollectionPage<>(totalElements, pageContent, toCriteria(criteria, totalElements)));
    }

    private List<DiffRowValue> getPageContent(List<VersionDataDiff> versionDataDiffs, Boolean isCountOnly,
                                              Set<String> changedAttributeNames, boolean isBackwardComparison) {
        List<DiffRowValue> result = new ArrayList<>();
        if (!TRUE.equals(isCountOnly))
            result = versionDataDiffs.stream()
                    .map(dataDiff -> new DiffRowValueCalculator(dataDiff.getFirstDiffRowValue(), dataDiff.getLastDiffRowValue(), changedAttributeNames, isBackwardComparison))
                    .map(DiffRowValueCalculator::calculate)
                    .collect(Collectors.toList());
        return result;
    }

    private List<String> getExcludedPrimaryValues(CompareDataCriteria criteria,
                                                  Set<String> changedAttributeNames,
                                                  boolean isBackwardComparison) {
        VersionDataDiffCriteria versionDataDiffCriteria = new VersionDataDiffCriteria();
        versionDataDiffCriteria.setOldVersionId(criteria.getOldVersionId());
        versionDataDiffCriteria.setNewVersionId(criteria.getNewVersionId());
        versionDataDiffCriteria.setPrimaryAttributesFilters(criteria.getPrimaryAttributesFilters());

        List<String> excludedPrimaryValues = new ArrayList<>();
        PageIterator<VersionDataDiff, VersionDataDiffCriteria> pageIterator =
                new PageIterator<>(versionDataDiffService::search, versionDataDiffCriteria);

        pageIterator.forEachRemaining(page -> {
            List<String> pageExcluded = page.getContent().stream()
                    .filter(dataDiff -> isExcluded(dataDiff, criteria, changedAttributeNames, isBackwardComparison))
                    .map(VersionDataDiff::getPrimaryValues)
                    .collect(Collectors.toList());
            excludedPrimaryValues.addAll(pageExcluded);
        });
        return excludedPrimaryValues;
    }

    private boolean isExcluded(VersionDataDiff dataDiff, CompareDataCriteria criteria,
                               Set<String> changedAttributeNames, boolean isBackwardComparison) {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(dataDiff.getFirstDiffRowValue(),
                dataDiff.getLastDiffRowValue(), changedAttributeNames, isBackwardComparison);

        boolean isAnnihilated = calculator.isAnnihilated();
        DiffStatusEnum filterStatus = criteria.getDiffStatus();
        boolean hasExcludedStatus = !isAnnihilated && nonNull(filterStatus) && filterStatus != calculator.calculate().getStatus();

        return isAnnihilated || hasExcludedStatus;
    }

    private Set<String> getAttributeDiffFieldNames(RefBookAttributeDiff refBookAttributeDiff) {
        Set<String> fieldNames = new HashSet<>();
        fieldNames.addAll(refBookAttributeDiff.getNewAttributes());
        fieldNames.addAll(refBookAttributeDiff.getUpdatedAttributes());
        fieldNames.addAll(refBookAttributeDiff.getOldAttributes());
        return fieldNames;
    }
}
