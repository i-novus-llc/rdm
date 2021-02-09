package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiff;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionDataDiffService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.RefBookVersionDiffEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.RefBookVersionDiffRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.VersionDataDiffRepository;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class VersionDataDiffServiceImpl implements VersionDataDiffService {

    public static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";

    private static final int VERSION_DATA_DIFF_PAGE_SIZE = 100;
    private static final String DATA_DIFF_PRIMARY_FORMAT = "%s=%s";

    private RefBookVersionRepository versionRepository;
    private RefBookVersionDiffRepository versionDiffRepository;
    private VersionDataDiffRepository dataDiffRepository;

    private CompareService compareService;

    private VersionValidation versionValidation;

    @Autowired
    public VersionDataDiffServiceImpl(RefBookVersionRepository versionRepository,
                                      RefBookVersionDiffRepository versionDiffRepository,
                                      VersionDataDiffRepository dataDiffRepository,
                                      CompareService compareService,
                                      VersionValidation versionValidation) {

        this.versionRepository = versionRepository;
        this.versionDiffRepository = versionDiffRepository;
        this.dataDiffRepository = dataDiffRepository;

        this.compareService = compareService;

        this.versionValidation = versionValidation;
    }

    @Override
    public Page<VersionDataDiff> search(CompareDataCriteria criteria) {
        return new PageImpl<>(emptyList(), criteria, 0);
    }

    @Override
    public void saveLastVersionDataDiff(String refBookCode) {

        versionValidation.validateRefBookCodeExists(refBookCode);

        // Две последние опубликованные версии:
        PageRequest pageRequest = PageRequest.of(RestCriteria.FIRST_PAGE_NUMBER, 2);
        List<RefBookVersionEntity> versionEntities = versionRepository
                .findByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED, pageRequest);
        if (isEmpty(versionEntities))
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
        return toDataDiffPrimary(diffFieldValue.getField().getName(), value);
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

    private String toDataDiffPrimary(String name, Object value) {
        return String.format(DATA_DIFF_PRIMARY_FORMAT, name, toDataDiffPrimaryValue(value));
    }

    private String toDataDiffPrimaryValue(Object value) {
        return (value instanceof String) ? StringUtils.toDoubleQuotes((String) value) : String.valueOf(value);
    }

    private String toDataDiffValues(DiffRowValue diffRowValue) {
        return JsonUtil.toJsonString(diffRowValue);
    }

    private DiffRowValue fromDataDiffValues(String dataDiffValues) {
        return JsonUtil.fromJsonString(dataDiffValues, DiffRowValue.class);
    }
}
