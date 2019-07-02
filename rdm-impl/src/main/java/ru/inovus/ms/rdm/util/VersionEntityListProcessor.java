package ru.inovus.ms.rdm.util;

import ru.inovus.ms.rdm.entity.RefBookVersionEntity;

import java.util.List;

@FunctionalInterface
public interface VersionEntityListProcessor {

    void process(List<RefBookVersionEntity> list);
}
