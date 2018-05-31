package ru.inovus.ms.rdm.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.model.Echo;
import ru.inovus.ms.rdm.service.EchoService;

/**
 * Created by tnurdinov on 30.05.2018.
 */
@Controller
public class EchoRest implements EchoService {

    @Autowired
    private EchoService echoService;

    @Override
    public Echo getEcho() {
        return echoService.getEcho();
    }
}
