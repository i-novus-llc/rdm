package ru.i_novus.ms.rdm.impl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionDataDiffService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.diff.RefBookVersionDiffEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.RefBookVersionDiffRepository;
import ru.i_novus.ms.rdm.impl.repository.diff.VersionDataDiffRepository;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class VersionDataDiffServiceImpl implements VersionDataDiffService {

    public static final int VERSION_DATA_DIFF_PAGE_SIZE = 100;
    private static final Logger logger = LoggerFactory.getLogger(VersionDataDiffServiceImpl.class);
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

    public void saveLastVersionDataDiff(String refBookCode) {

        versionValidation.validateRefBookCodeExists(refBookCode);

        // Две последние опубликованные версии:
        PageRequest pageRequest = PageRequest.of(AbstractCriteria.FIRST_PAGE_NUMBER, 2);
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
        versionDiffEntity = versionDiffRepository.save(versionDiffEntity);

        try {
            saveDataDiff(versionDiffEntity);

        } catch (Exception e) {
            versionDiffRepository.delete(versionDiffEntity);
            throw e;
        }
    }

    private void saveDataDiff(RefBookVersionDiffEntity versionDiffEntity) {
        
        CompareDataCriteria compareCriteria = new CompareDataCriteria(
                versionDiffEntity.getOldVersion().getId(),
                versionDiffEntity.getNewVersion().getId()
        );
        compareCriteria.setPageSize(VERSION_DATA_DIFF_PAGE_SIZE);

        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(
                pageCriteria -> compareService.compareData(pageCriteria).getRows(), compareCriteria, true);
        pageIterator.forEachRemaining(page -> {
            // todo
            throw new NotFoundException("DEBUG call exception");
        });
    }
}
