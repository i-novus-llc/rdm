package ru.i_novus.ms.rdm.impl.service.diff;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.provider.PublishResolver;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.service.diff.DataDiffUtil.toDataDiffValues;

@Component
@SuppressWarnings({"rawtypes","java:S3740"})
public class StoreDataDiffResolver implements PublishResolver {

    private static final Logger logger = LoggerFactory.getLogger(StoreDataDiffResolver.class);

    private static final int VERSION_DATA_DIFF_PAGE_SIZE = 100;

    private final RefBookVersionRepository versionRepository;
    private final RefBookVersionDiffRepository versionDiffRepository;
    private final VersionDataDiffRepository dataDiffRepository;

    private final CompareService compareService;

    private final VersionValidation versionValidation;

    @Autowired
    public StoreDataDiffResolver(RefBookVersionRepository versionRepository,
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

    public void resolve(String refBookCode) {
        try {
            saveLastVersionDataDiff(refBookCode);

        } catch (RuntimeException e) {
            logger.error("Save last version data diff error.");
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Сохранение результата сравнения после публикации справочника.
     *
     * @param refBookCode код справочника
     */
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

        if (!oldVersion.getStructure().hasPrimary() ||
                !newVersion.getStructure().hasPrimary())
            return;

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
        compareCriteria.setUseCached(Boolean.FALSE);
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
        return DataDiffUtil.toPrimaryString(diffFieldValue.getField().getName(), value);
    }
}
