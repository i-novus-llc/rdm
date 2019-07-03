package ru.inovus.ms.rdm.provider;

import java.util.List;

@FunctionalInterface
public interface ListProcessor<T> {

    void process(List<T> list);
}
