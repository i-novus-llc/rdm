package ru.inovus.ms.rdm.provider;

import ru.inovus.ms.rdm.model.version.RefBookVersion;

import java.util.List;

@FunctionalInterface
public interface RefBookVersionListProcessor {

    void process(List<RefBookVersion> list);
}
