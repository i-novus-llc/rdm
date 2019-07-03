package ru.inovus.ms.rdm.provider;

import java.util.List;

@FunctionalInterface
public interface ListProcessor<T> {

    boolean process(List<T> list);
}
