package ru.i_novus.ms.rdm.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiffCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionDataDiffService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.RefBookVersionDiffEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffResult;
import ru.i_novus.ms.rdm.impl.provider.VdsMapperConfigurer;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.RefBookVersionDiffRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.VersionDataDiffRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.VersionDataDiffResultRepository;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Pageable.unpaged;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.StringUtils.addSingleQuotes;
import static ru.i_novus.ms.rdm.api.util.StringUtils.toDoubleQuotes;

@Service
@Primary
@SuppressWarnings({"rawtypes", "java:S3740"})
public class VersionDataDiffServiceImpl implements VersionDataDiffService {

    public static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    public static final String COMPARE_DATA_DIFF_NOT_FOUND_EXCEPTION_CODE = "compare.data.diff.not.found";
    public static final String COMPARE_PRIMARY_FILTER_IS_EXACT_ONLY_EXCEPTION_CODE = "compare.primary.filter.is.exact.only";

    private static final int VERSION_DATA_DIFF_PAGE_SIZE = 100;
    private static final String DATA_DIFF_PRIMARY_FORMAT = "%s=%s";

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private static final ObjectMapper vdsObjectMapper = createVdsObjectMapper();

    private RefBookVersionRepository versionRepository;
    private RefBookVersionDiffRepository versionDiffRepository;
    private VersionDataDiffRepository dataDiffRepository;
    private VersionDataDiffResultRepository dataDiffResultRepository;

    private CompareService compareService;

    private VersionValidation versionValidation;

    @Autowired
    public VersionDataDiffServiceImpl(RefBookVersionRepository versionRepository,
                                      RefBookVersionDiffRepository versionDiffRepository,
                                      VersionDataDiffRepository dataDiffRepository,
                                      VersionDataDiffResultRepository dataDiffResultRepository,
                                      CompareService compareService,
                                      VersionValidation versionValidation) {

        this.versionRepository = versionRepository;
        this.versionDiffRepository = versionDiffRepository;
        this.dataDiffRepository = dataDiffRepository;
        this.dataDiffResultRepository = dataDiffResultRepository;

        this.compareService = compareService;

        this.versionValidation = versionValidation;
    }

    @Override
    public Page<VersionDataDiff> search(VersionDataDiffCriteria criteria) {

        List<RefBookVersionEntity> comparedEntities = getVersions(criteria.getOldVersionId(), criteria.getNewVersionId());
        RefBookVersionEntity newVersion = comparedEntities.get(0);
        RefBookVersionEntity oldVersion = comparedEntities.get(1);
        String refBookCode = newVersion.getRefBook().getCode();
        String versionIds = getVersionIds(refBookCode, oldVersion.getId(), newVersion.getId());

        String versionDiffIds = searchVersionDiffIds(refBookCode, oldVersion, newVersion, versionIds);
        return searchDataDiffs(criteria, versionDiffIds);
    }

    private String getVersionIds(String refBookCode, Integer oldVersionId, Integer newVersionId) {
        
        List<RefBookVersionEntity> versionEntities = versionRepository
                .findByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED, unpaged());

        List<Integer> versionIds = versionEntities.stream()
                .map(RefBookVersionEntity::getId)
                .dropWhile(id -> !newVersionId.equals(id))
                .takeWhile(id -> !oldVersionId.equals(id))
                .collect(toList());
        versionIds.add(oldVersionId);

        return versionIds.stream().map(String::valueOf).collect(joining(","));
    }

    private String searchVersionDiffIds(String refBookCode, RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion, String versionIds) {

        String result = versionDiffRepository.searchVersionDiffIds(oldVersion.getId(), newVersion.getId(), versionIds);
        if (isEmpty(result)) {
            throw new NotFoundException(new Message(COMPARE_DATA_DIFF_NOT_FOUND_EXCEPTION_CODE,
                    refBookCode, oldVersion.getVersionNumber(), newVersion.getVersionNumber()));
        }

        return result;
    }

    private Page<VersionDataDiff> searchDataDiffs(VersionDataDiffCriteria criteria, String versionDiffIds) {
        
        String includePrimaries = toIncludePrimaries(criteria.getPrimaryAttributesFilters());
        String excludePrimaries = toExcludePrimaries(criteria.getExcludePrimaryValues());

        Page<VersionDataDiffResult> diffs = dataDiffResultRepository
                .searchByVersionDiffs(versionDiffIds, includePrimaries, excludePrimaries, criteria);
        if (diffs == null || isEmpty(diffs.getContent()))
            return new PageImpl<>(emptyList(), criteria, 0);

        List<VersionDataDiff> dataDiffs = diffs.stream().map(this::toVersionDataDiff).collect(toList());
        return new PageImpl<>(dataDiffs, criteria, diffs.getTotalElements());
    }

    private String toIncludePrimaries(Set<List<AttributeFilter>> primaryAttributesFilters) {

        if (isEmpty(primaryAttributesFilters))
            return "";

        List<String> result = primaryAttributesFilters.stream()
                .map(this::toAttributeFilterPrimaries)
                .filter(Objects::nonNull)
                .collect(toList());

        return toFlatPrimaries(result);
    }

    private String toAttributeFilterPrimaries(List<AttributeFilter> primaryAttributesFilter) {

        if (isEmpty(primaryAttributesFilter))
            return null;

        if (primaryAttributesFilter.stream().anyMatch(this::isAttributeFilterDisallowed))
            throw new IllegalArgumentException(COMPARE_PRIMARY_FILTER_IS_EXACT_ONLY_EXCEPTION_CODE);

        return primaryAttributesFilter.stream()
                .map(this::toAttributeFilterPrimary)
                .sorted()
                .collect(joining(", "));
    }

    private boolean isAttributeFilterDisallowed(AttributeFilter primaryAttributeFilter) {
        return !SearchTypeEnum.EXACT.equals(primaryAttributeFilter.getSearchType());
    }

    private String toAttributeFilterPrimary(AttributeFilter primaryAttributeFilter) {
        return toNameValuePrimary(primaryAttributeFilter.getAttributeName(), primaryAttributeFilter.getValue());
    }

    private String toExcludePrimaries(List<String> excludePrimaryValues) {
        return toFlatPrimaries(excludePrimaryValues);
    }

    private String toFlatPrimaries(List<String> primaryValues) {

        if (isEmpty(primaryValues))
            return "";

        return primaryValues.stream().map(StringUtils::toDoubleQuotes).collect(joining(","));
    }

    private VersionDataDiff toVersionDataDiff(VersionDataDiffResult diff) {

        return new VersionDataDiff(
                diff.getPrimaryValues(),
                fromDataDiffValues(diff.getFirstDiffValues()),
                fromDataDiffValues(diff.getLastDiffValues())
        );
    }

    @Override
    public void saveLastVersionDataDiff(String refBookCode) {

        versionValidation.validateRefBookCodeExists(refBookCode);

        // Две последние опубликованные версии:
        PageRequest pageRequest = PageRequest.of(RestCriteria.FIRST_PAGE_NUMBER, 2);
        List<RefBookVersionEntity> versionEntities = versionRepository
                .findByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED, pageRequest);
        if (org.springframework.util.CollectionUtils.isEmpty(versionEntities))
            throw new NotFoundException(String.format("Two last published versions of refBook '%s' not found", refBookCode));
        if (versionEntities.size() == 1)
            return; // First published version, no data diff.

        saveVersionDataDiff(versionEntities.get(1), versionEntities.get(0));
    }

    private void saveVersionDataDiff(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion) {

        RefBookVersionDiffEntity versionDiffEntity = new RefBookVersionDiffEntity(oldVersion, newVersion);
        versionDiffEntity = versionDiffRepository.saveAndFlush(versionDiffEntity);

        try {
            saveDataDiff(versionDiffEntity);

        } catch (Exception e) {
            versionDiffRepository.delete(versionDiffEntity);
            versionDiffRepository.flush();

            throw e;
        }
    }

    private void saveDataDiff(RefBookVersionDiffEntity versionDiffEntity) {

        CompareDataCriteria compareCriteria = new CompareDataCriteria(
                versionDiffEntity.getOldVersion().getId(),
                versionDiffEntity.getNewVersion().getId()
        );
        compareCriteria.setPageSize(VERSION_DATA_DIFF_PAGE_SIZE);

        List<String> primaries = versionDiffEntity.getOldVersion().getStructure().getPrimaryCodes();

        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(
                pageCriteria -> compareService.compareData(pageCriteria).getRows(), compareCriteria, true);
        pageIterator.forEachRemaining(page -> {
            List<VersionDataDiffEntity> dataDiffEntities = toDataDiffEntities(versionDiffEntity, page, primaries);
            dataDiffRepository.saveAll(dataDiffEntities);
            dataDiffRepository.flush();
        });
    }

    private List<VersionDataDiffEntity> toDataDiffEntities(RefBookVersionDiffEntity versionDiffEntity,
                                                           Page<? extends DiffRowValue> diffRowValues,
                                                           List<String> primaries) {
        return diffRowValues.stream()
                .map(diffRowValue -> toDataDiffEntity(versionDiffEntity, diffRowValue, primaries))
                .collect(toList());
    }

    private VersionDataDiffEntity toDataDiffEntity(RefBookVersionDiffEntity versionDiffEntity,
                                                   DiffRowValue diffRowValue,
                                                   List<String> primaries) {

        VersionDataDiffEntity dataDiffEntity = new VersionDataDiffEntity();
        dataDiffEntity.setVersionDiffEntity(versionDiffEntity);
        dataDiffEntity.setPrimaries(toDataDiffPrimaries(diffRowValue, primaries));
        dataDiffEntity.setValues(toDataDiffValues(diffRowValue));

        return dataDiffEntity;
    }

    private String toDataDiffPrimaries(DiffRowValue diffRowValue, List<String> primaries) {

        return diffRowValue.getValues().stream()
                .filter(diffFieldValue -> primaries.contains(diffFieldValue.getField().getName()))
                .map(this::toDataDiffPrimary)
                .sorted()
                .collect(joining(", "));
    }

    private String toDataDiffPrimary(DiffFieldValue diffFieldValue) {

        Object value = diffFieldValue.getNewValue() != null ? diffFieldValue.getNewValue() : diffFieldValue.getOldValue();
        return toNameValuePrimary(diffFieldValue.getField().getName(), value);
    }

    @Override
    public Boolean isPublishedBefore(Integer versionId1, Integer versionId2) {

        List<RefBookVersionEntity> entities = getVersions(versionId1, versionId2);
        return versionId2.equals(entities.get(0).getId());
    }

    private List<RefBookVersionEntity> getVersions(Integer versionId1, Integer versionId2) {

        List<RefBookVersionEntity> entities = (versionId1 != null && versionId2 != null)
                ? versionRepository.findByIdInAndStatusOrderByFromDateDesc(
                        List.of(versionId1, versionId2), RefBookVersionStatus.PUBLISHED)
                : emptyList();

        validateVersionExists(versionId1, entities);
        validateVersionExists(versionId2, entities);

        return entities;
    }

    /**  Проверка существования версии по идентификатору в списке версий. */
    private void validateVersionExists(Integer versionId, List<RefBookVersionEntity> entities) {

        if (versionId == null ||
                entities.stream().noneMatch(e -> versionId.equals(e.getId())))
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));
    }

    private String toNameValuePrimary(String name, Object value) {
        return String.format(DATA_DIFF_PRIMARY_FORMAT, name, toPrimaryValue(value));
    }

    private String toPrimaryValue(Object value) {

        if (value instanceof LocalDate)
            return addSingleQuotes(DATE_FORMATTER.format((LocalDate) value));

        if (value instanceof String)
            return toDoubleQuotes((String) value);

        return String.valueOf(value);
    }

    private String toDataDiffValues(DiffRowValue diffRowValue) {
        return JsonUtil.toJsonString(vdsObjectMapper, diffRowValue);
    }

    private DiffRowValue fromDataDiffValues(String dataDiffValues) {
        return (dataDiffValues != null) ? JsonUtil.fromJsonString(vdsObjectMapper, dataDiffValues, DiffRowValue.class) : null;
    }

    private static ObjectMapper createVdsObjectMapper() {

        ObjectMapper jsonMapper = new ObjectMapper();
        new VdsMapperConfigurer().configure(jsonMapper);

        return jsonMapper;
    }
}
