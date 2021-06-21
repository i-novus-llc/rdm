package ru.i_novus.ms.rdm.api.util;

public interface VersionNumberStrategy {

    String first();

    String next(Integer refBookId);

    boolean check(String version, Integer refBookId);
}
