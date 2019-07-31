package ru.inovus.ms.rdm.util;

public interface VersionNumberStrategy {

    String next(Integer refBookId);

    boolean check(String version, Integer refBookId);
}
