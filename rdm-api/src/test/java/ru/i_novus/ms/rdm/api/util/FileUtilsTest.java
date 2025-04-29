package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static ru.i_novus.ms.rdm.api.util.FileUtils.*;

public class FileUtilsTest {

    @Test
    public void testGetLastExtension() {

        final String filepath = "path\\to\\";

        assertEquals("", getLastExtension(null));
        assertEquals("", getLastExtension(""));
        assertEquals("", getLastExtension(filepath));
        assertEquals("ext", getLastExtension(filepath + "filename.ext"));
        assertEquals(" spaced", getLastExtension(filepath + "filename. spaced"));
        assertEquals("last", getLastExtension(filepath + "filename.first.last"));
    }

    @Test
    public void testGetRefBookFileExtension() {

        final String filepath = "path\\to\\";

        assertEquals("", getRefBookFileExtension(null));
        assertEquals("", getRefBookFileExtension(""));
        assertEquals("", getRefBookFileExtension(filepath));
        assertEquals("EXT", getRefBookFileExtension(filepath + "filename.ext"));
        assertEquals("SPACED", getRefBookFileExtension(filepath + "filename. spaced"));
        assertEquals("LAST", getRefBookFileExtension(filepath + "filename.first.last"));
    }

    @Test
    public void testGetExtensions() {

        final String filepath = "path\\to\\";

        assertEquals(emptyList(), getExtensions(null));
        assertEquals(emptyList(), getExtensions(""));
        assertEquals(emptyList(), getExtensions(filepath));
        assertEquals(List.of("ext"), getExtensions(filepath + "filename.ext"));
        assertEquals(List.of("ext2", " ext1"), getExtensions(filepath + "filename. ext1.ext2"));
        assertEquals(List.of("ext2", "3", "2", "1"), getExtensions(filepath + "filename.1.2.3.ext2"));
    }

}