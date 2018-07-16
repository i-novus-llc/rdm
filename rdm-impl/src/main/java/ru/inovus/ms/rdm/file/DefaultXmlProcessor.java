package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.Result;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Created by tnurdinov on 24.05.2018.
 */
public class DefaultXmlProcessor implements FileProcessor {

    @Override
    public Result process(Supplier<InputStream> fileSource) {
        return null;
    }
}
