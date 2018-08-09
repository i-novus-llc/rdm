package ru.inovus.ms.rdm.file.export;

import java.io.OutputStream;

public interface FileGenerator {

    void generate(OutputStream outputStream);

}
