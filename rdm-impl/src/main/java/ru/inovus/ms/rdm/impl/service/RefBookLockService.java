package ru.inovus.ms.rdm.impl.service;

/**
 * Created by znurgaliev on 19.09.2018.
 */
public interface RefBookLockService {

    void setRefBookPublishing(Integer refBookId);

    void setRefBookUpdating(Integer refBookId);

    void deleteRefBookOperation(Integer refBookId);

    void validateRefBookNotBusyByVersionId(Integer versionId);

    void validateRefBookNotBusyByRefBookId(Integer refBookId);

}
