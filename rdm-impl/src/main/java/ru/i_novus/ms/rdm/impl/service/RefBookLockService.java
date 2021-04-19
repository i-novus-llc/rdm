package ru.i_novus.ms.rdm.impl.service;

/**
 * Created by znurgaliev on 19.09.2018.
 */
public interface RefBookLockService {

    void setRefBookPublishing(Integer refBookId);

    void setRefBookUpdating(Integer refBookId);

    void deleteRefBookOperation(Integer refBookId);

    void validateRefBookNotBusyByRefBookId(Integer refBookId);
}
