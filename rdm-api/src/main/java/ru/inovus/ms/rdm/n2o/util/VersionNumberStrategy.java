package ru.inovus.ms.rdm.n2o.util;

public interface VersionNumberStrategy {

    String next(Integer refBookId);

    boolean check(String version, Integer refBookId);
}
