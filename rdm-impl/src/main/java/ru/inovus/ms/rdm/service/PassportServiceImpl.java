package ru.inovus.ms.rdm.service;

import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.Passport;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
public class PassportServiceImpl implements PassportService {
    @Override
    public Long create(Long draftId, Passport passport) {
        return null;
    }

    @Override
    public void update(Long passportId, Passport passport) {

    }

    @Override
    public Passport get(Long passportId) {
        return null;
    }
}
