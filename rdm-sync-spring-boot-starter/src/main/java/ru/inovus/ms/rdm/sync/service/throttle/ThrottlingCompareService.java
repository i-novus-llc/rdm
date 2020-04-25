package ru.inovus.ms.rdm.sync.service.throttle;

import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.compare.ComparableRow;
import ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.api.model.diff.PassportDiff;
import ru.inovus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.inovus.ms.rdm.api.model.diff.StructureDiff;
import ru.inovus.ms.rdm.api.service.CompareService;

public class ThrottlingCompareService implements CompareService {

    private final Throttle throttle;
    private final CompareService compareService;

    public ThrottlingCompareService(Throttle throttle, CompareService compareService) {
        this.throttle = throttle;
        this.compareService = compareService;
    }

    public PassportDiff comparePassports(Integer oldVersionId, Integer newVersionId) {
        throttle.throttleAndUpdateRequestTime();
        return compareService.comparePassports(oldVersionId, newVersionId);
    }

    public StructureDiff compareStructures(Structure oldStructure, Structure newStructure) {
        throttle.throttleAndUpdateRequestTime();
        return compareService.compareStructures(oldStructure, newStructure);
    }

    public StructureDiff compareStructures(Integer oldVersionId, Integer newVersionId) {
        throttle.throttleAndUpdateRequestTime();
        return compareService.compareStructures(oldVersionId, newVersionId);
    }

    public RefBookDataDiff compareData(CompareDataCriteria compareDataCriteria) {
        throttle.throttleAndUpdateRequestTime();
        return compareService.compareData(compareDataCriteria);
    }

    public Page<ComparableRow> getCommonComparableRows(CompareDataCriteria criteria) {
        throttle.throttleAndUpdateRequestTime();
        return compareService.getCommonComparableRows(criteria);
    }

}
