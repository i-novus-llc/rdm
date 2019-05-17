package ru.inovus.ms.rdm.file.process;

import java.io.InputStream;
import java.util.function.Supplier;

public interface FileProcessor<T> {
    T process(Supplier<InputStream> fileSource);
}
