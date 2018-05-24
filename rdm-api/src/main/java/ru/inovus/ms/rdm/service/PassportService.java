package ru.inovus.ms.rdm.service;

import ru.inovus.ms.rdm.model.Passport;

public interface PassportService {
    /***
     *
     * @param draftId //todo draftId is a part of Passport?
     * @param passport
     * @return passportId
     */
    Long create(Long draftId, Passport passport);

    void update(Long passportId, Passport passport);

    Passport get(Long passportId);

}
