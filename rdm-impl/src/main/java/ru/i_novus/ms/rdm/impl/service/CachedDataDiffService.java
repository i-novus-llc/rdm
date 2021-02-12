package ru.i_novus.ms.rdm.impl.service;

import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookAttributeDiff;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;

public interface CachedDataDiffService {

    DataDifference getCachedDataDifference(CompareDataCriteria criteria, RefBookAttributeDiff refBookAttributeDiff);

}
