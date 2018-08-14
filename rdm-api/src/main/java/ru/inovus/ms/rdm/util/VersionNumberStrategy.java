package ru.inovus.ms.rdm.util;

public interface VersionNumberStrategy {

    String next(Integer refbookId);

    boolean check(String version, Integer refbookId);
}
