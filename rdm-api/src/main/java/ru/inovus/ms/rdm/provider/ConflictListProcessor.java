package ru.inovus.ms.rdm.provider;

import ru.inovus.ms.rdm.model.conflict.Conflict;

import java.util.List;

@FunctionalInterface
public interface ConflictListProcessor {

    void process(List<Conflict> list);
}
