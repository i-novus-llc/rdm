package ru.inovus.ms.rdm.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.Conflict;
import ru.inovus.ms.rdm.service.api.ConflictService;

import java.util.List;

import static java.util.Collections.emptyList;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    @Override
    public List<Conflict> calculateConflicts(Integer refFromId, Integer refToId) {
        return emptyList();
    }

}
