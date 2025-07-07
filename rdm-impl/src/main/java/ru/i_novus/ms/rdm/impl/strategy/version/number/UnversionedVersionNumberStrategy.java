package ru.i_novus.ms.rdm.impl.strategy.version.number;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UnversionedVersionNumberStrategy implements VersionNumberStrategy {

    private static final String USED_VERSION = "-1.0";

    @Override
    public String first() {
        return USED_VERSION;
    }

    @Override
    @Transactional
    public String next(Integer refBookId) {
        return USED_VERSION;
    }

    @Override
    public boolean check(String version, Integer refBookId) {
        return version != null && version.equals(USED_VERSION);
    }
}
