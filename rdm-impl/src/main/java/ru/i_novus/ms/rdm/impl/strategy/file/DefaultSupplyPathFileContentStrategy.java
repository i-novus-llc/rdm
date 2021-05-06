package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.file.FileStorage;

import java.io.InputStream;
import java.util.function.Supplier;

@Component
public class DefaultSupplyPathFileContentStrategy implements SupplyPathFileContentStrategy {

    @Autowired
    private FileStorage fileStorage;

    @Override
    public Supplier<InputStream> supply(String filePath) {

        return () -> fileStorage.getContent(filePath);
    }
}
