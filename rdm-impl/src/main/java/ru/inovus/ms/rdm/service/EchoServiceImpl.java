package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.entity.EchoEntity;
import ru.inovus.ms.rdm.model.Echo;
import ru.inovus.ms.rdm.repositiory.EchoRepository;

/**
 * Created by tnurdinov on 30.05.2018.
 */
@Service
@Transactional
public class EchoServiceImpl implements EchoService {

    @Autowired
    private EchoRepository echoRepository;

    @Override
    public Echo getEcho() {
        EchoEntity entity = echoRepository.findOne(1l);
        Echo echo = new Echo();
        echo.setValue(entity.getValue());
        return echo;
    }
}
