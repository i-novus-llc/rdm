package ru.i_novus.ms.rdm.impl.strategy.version.number;

public interface VersionNumberStrategy {

    String first();

    String next(Integer refBookId);

    boolean check(String version, Integer refBookId);
}
