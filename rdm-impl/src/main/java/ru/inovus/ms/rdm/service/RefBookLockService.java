package ru.inovus.ms.rdm.service;

/**
 * Created by znurgaliev on 19.09.2018.
 */
public interface RefBookLockService {

    void cleanOperations();

    void setRefBookPublishing(Integer refBookId);

    void setRefBookUploading(Integer refBookId);

    void deleteRefBookOperation(Integer refBookId);

    void validateRefBookNotBusyByVersionId(Integer versionId);

    void validateRefBookNotBusyByRefBookId(Integer refBookId);
}
