package ru.i_novus.ms.rdm.n2o.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileUsageTypeEnum;
import ru.i_novus.ms.rdm.api.exception.FileException;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class FileValidationTest {

    private static final int MAX_FILE_SIZE_MB = 10;
    private static final long MEGABYTE = 1024 * 1024;

    @InjectMocks
    private FileValidationImpl validation;

    @Before
    public void setUp() {

        validation.setMaxFileSizeMb(MAX_FILE_SIZE_MB);
    }

    @Test
    public void testValidateName() {

        validation.validateName("rebook");
        validation.validateName("rebook.xml");
        validation.validateName("path/to/rebook.xml");
    }

    @Test
    public void testValidateNameWhenAbsent() {

        testValidateNameWhenAbsent(null);
        testValidateNameWhenAbsent("");
        testValidateNameWhenAbsent(" ");
    }

    private void testValidateNameWhenAbsent(String filename) {

        try {
            validation.validateName(filename);
            fail("File name is absent for '" + filename + "'");

        } catch (FileException e) {
            assertEquals("file.name.absent", e.getCode());
            assertNull(e.getArgs());
        }
    }

    @Test
    public void testValidateExtensions() {

        validation.validateExtensions("rebook.xml");
        validation.validateExtensions("v.1.23.456.xml");
    }

    @Test
    public void testValidateExtensionsWhenAbsent() {

        testValidateExtensionsWhenAbsent(null);
        testValidateExtensionsWhenAbsent("");
        testValidateExtensionsWhenAbsent("  ");
        testValidateExtensionsWhenAbsent("refbook");
        testValidateExtensionsWhenAbsent("refbook.");
        testValidateExtensionsWhenAbsent("refbook.  ");
    }

    private void testValidateExtensionsWhenAbsent(String filename) {
        try {
            validation.validateExtensions(filename);
            fail("File extension is absent for '" + filename + "'");

        } catch (FileException e) {
            assertEquals("file.extension.absent", e.getCode());
            assertEquals(filename, e.getArgs()[0]);
        }
    }

    @Test
    public void testValidateExtensionsWhenInvalid() {

        testValidateExtensionsWhenInvalid("refbook.exe.xml");
        testValidateExtensionsWhenInvalid("refbook.htm.xml");
    }

    private void testValidateExtensionsWhenInvalid(String filename) {
        try {
            validation.validateExtensions(filename);
            fail("Some file extension is invalid for '" + filename + "'");

        } catch (FileException e) {
            assertEquals("file.extension.invalid", e.getCode());
            assertEquals(1, e.getArgs().length);
        }
    }

    @Test
    public void testValidateExtensionByUsage() {

        validation.validateExtensionByUsage("XML", FileUsageTypeEnum.REF_BOOK);

        validation.validateExtensionByUsage("XML", FileUsageTypeEnum.REF_DRAFT);
        validation.validateExtensionByUsage("XLSX", FileUsageTypeEnum.REF_DRAFT);

        validation.validateExtensionByUsage("XML", FileUsageTypeEnum.REF_DATA);
        validation.validateExtensionByUsage("XLSX", FileUsageTypeEnum.REF_DATA);
    }

    @Test
    public void testValidateExtensionByUsageWhenInvalid() {

        testValidateExtensionByUsageWhenInvalid("XLSX", FileUsageTypeEnum.REF_BOOK);

        testValidateExtensionByUsageWhenInvalid("CSV", FileUsageTypeEnum.REF_BOOK);
        testValidateExtensionByUsageWhenInvalid("CSV", FileUsageTypeEnum.REF_DRAFT);
        testValidateExtensionByUsageWhenInvalid("CSV", FileUsageTypeEnum.REF_DATA);
    }

    private void testValidateExtensionByUsageWhenInvalid(String extension, FileUsageTypeEnum fileUsageType) {
        try {
            validation.validateExtensionByUsage(extension, fileUsageType);
            fail("Some file extension is invalid for '" + extension + "' for usage");

        } catch (FileException e) {
            assertEquals("file.extension.invalid", e.getCode());
            assertEquals(extension, e.getArgs()[0]);
        }
    }

    @Test
    public void testValidateSize() {

        final long fileSize = 4 * MEGABYTE;

        validation.validateSize(fileSize);
    }

    @Test
    public void testValidateSizeWhenMoreThenLimit() {

        final long fileSize = 100 * MEGABYTE;
        try {
            validation.validateSize(fileSize);
            fail("File size is invalid");

        } catch (FileException e) {
            assertEquals("file.is.too.big", e.getCode());
            assertEquals(MAX_FILE_SIZE_MB, e.getArgs()[0]);
        }
    }
}