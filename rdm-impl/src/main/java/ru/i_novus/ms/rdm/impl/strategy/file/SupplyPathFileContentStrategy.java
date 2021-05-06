package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.impl.strategy.Strategy;

import java.io.InputStream;
import java.util.function.Supplier;

public interface SupplyPathFileContentStrategy extends Strategy {

    Supplier<InputStream> supply(String filePath);
}
