package ru.i_novus.ms.rdm.api.audit;

import ru.i_novus.ms.rdm.api.audit.model.AuditClientRequest;

public interface AuditClient {

    void add(AuditClientRequest request);
}