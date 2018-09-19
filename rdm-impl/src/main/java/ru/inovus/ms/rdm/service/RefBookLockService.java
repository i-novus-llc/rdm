package ru.inovus.ms.rdm.service;

import ru.inovus.ms.rdm.entity.RefBookEntity;

/**
 * Created by znurgaliev on 19.09.2018.
 */
public interface RefBookLockService {

    void cleanOperations();

    void setRefBookPublishing(Integer refBookId);

    void setRefBookUploading(Integer refBookId);

    void deleteRefBookAction(Integer refBookId);

    void validateRefBookNotBusyByVersionId(Integer versionId);

    void validateRefBookNotBusy(RefBookEntity refBookEntity);
}
