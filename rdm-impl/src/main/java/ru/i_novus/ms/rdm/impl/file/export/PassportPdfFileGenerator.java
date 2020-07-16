package ru.i_novus.ms.rdm.impl.file.export;

import com.itextpdf.text.DocumentException;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.util.CollectionUtils.isEmpty;

public class PassportPdfFileGenerator implements FileGenerator {

    private static final String VERSION_CODE_NAME = "Код справочника";
    private static final String VERSION_CATEGORY_NAME = "Категория справочника";

    private PassportValueRepository passportValueRepository;

    /** Версия справочника. */
    private RefBookVersion version;

    /** Заголовок. */
    private String head;

    public PassportPdfFileGenerator(PassportValueRepository passportValueRepository,
                                    RefBookVersion version, String head) {
        this.passportValueRepository = passportValueRepository;

        this.version = version;
        this.head = head;
    }

    @Override
    public void generate(OutputStream outputStream) {

        List<PassportValueEntity> values = passportValueRepository
                .findAllByVersionIdOrderByAttributePosition(version.getId());
        if (isEmpty(values)) return;

        Map<String, String> passportToWrite = new LinkedHashMap<>();

        passportToWrite.put(VERSION_CODE_NAME, version.getCode());
        if (!StringUtils.isEmpty(version.getCategory()))
            passportToWrite.put(VERSION_CATEGORY_NAME, version.getCategory());

        for (PassportValueEntity value : values) {
            passportToWrite.put(value.getAttribute().getName(), String.valueOf(value.getValue()));
        }

        String paragraph = values.stream()
                .filter(value -> Objects.equals(value.getAttribute().getCode(), head))
                .map(value -> String.valueOf(value.getValue()))
                .findFirst().orElse("");

        PdfCreatorUtil pdfCreatorUtil = new PdfCreatorUtil();
        try {
            pdfCreatorUtil.writeDocument(outputStream, passportToWrite, paragraph);

        } catch (DocumentException | IOException e) {
            throw new RdmException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // No close for generator
    }
}