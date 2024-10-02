package ru.i_novus.ms.rdm.api.audit.impl;

import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.audit.AuditClient;
import ru.i_novus.ms.rdm.api.audit.model.AuditClientRequest;

/**
 * Копия:
 * Класс-заглушка клиента для настройки audit.client.enabled=false
 */
@Service
public class StubAuditClientImpl implements AuditClient {

    @Override
    public void add(AuditClientRequest request) {
        //stub, do nothing
    }
}
