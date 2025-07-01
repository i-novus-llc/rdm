package ru.i_novus.ms.rdm.n2o.validation;

import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.FileUsageTypeEnum;
import ru.i_novus.ms.rdm.api.exception.FileException;
import ru.i_novus.ms.rdm.api.util.FileUtils;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNumeric;
import static ru.i_novus.ms.rdm.api.exception.FileException.*;

@Component
public class FileValidationImpl implements FileValidation {

    private static final String FILE_IS_TOO_BIG_EXCEPTION_CODE = "file.is.too.big";

    private static final long KILOBYTE = 1024;

    @Value("${rdm.max-file-size-mb:55}")
    private int maxFileSizeMb;

    public void setMaxFileSizeMb(int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    @Override
    public void validateName(String filename) {

        if (!StringUtils.hasText(filename))
            throw newAbsentFileNameException();
    }

    @Override
    public void validateExtensions(String filename) {

        final List<String> extensions = FileUtils.getExtensions(filename);

        if (CollectionUtils.isEmpty(extensions))
            throw newAbsentFileExtensionException(filename);

        final String lastExtension = extensions.get(0);
        if (!StringUtils.hasText(lastExtension))
            throw newAbsentFileExtensionException(filename);

        extensions.stream()
                .skip(1)
                .filter(this::isInvalidExtension)
                .findAny()
                .ifPresent(extension -> { throw newInvalidFileExtensionException(extension); });
    }

    private boolean isInvalidExtension(String extension) {

        return StringUtils.hasText(extension) &&
                !isNumeric(extension); // Допустим только номер версии
    }

    @Override
    public void validateExtensionByUsage(String extension, FileUsageTypeEnum fileUsageType) {

        if (fileUsageType == null)
            return;

        if (!fileUsageType.getExtensions().contains(extension))
            throw newInvalidFileExtensionException(extension);
    }

    @Override
    public void validateSize(long fileSize) {

        if (toMbSize(fileSize) > maxFileSizeMb)
            throw new FileException(new Message(FILE_IS_TOO_BIG_EXCEPTION_CODE, maxFileSizeMb));
    }

    private long toMbSize(long size) {

        return size / KILOBYTE / KILOBYTE;
    }
}
