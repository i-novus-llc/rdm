package ru.inovus.ms.rdm.esnsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EsnsiIntegrationServiceImpl implements EsnsiIntegrationService {

    /**
     * Идентификаторы справочников, которые забираем из ЕСНСИ.
     */
    private static final List<String> CODES = List.of("01-519", "01-245");

    @Autowired
    private EsnsiSmevClient esnsiClient;

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

}
