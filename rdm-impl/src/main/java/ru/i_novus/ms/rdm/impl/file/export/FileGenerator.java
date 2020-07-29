package ru.i_novus.ms.rdm.impl.file.export;

import java.io.Closeable;
import java.io.OutputStream;

public interface FileGenerator extends Closeable {

    void generate(OutputStream outputStream);

}
