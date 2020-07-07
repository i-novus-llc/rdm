package ru.inovus.ms.rdm.api.util;

public interface VersionNumberStrategy {

    String next(Integer refBookId);

    boolean check(String version, Integer refBookId);
}
