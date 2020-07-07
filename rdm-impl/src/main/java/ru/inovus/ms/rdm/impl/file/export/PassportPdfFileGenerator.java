package ru.inovus.ms.rdm.impl.file.export;

import com.itextpdf.text.DocumentException;
import ru.inovus.ms.rdm.impl.entity.PassportValueEntity;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.impl.repository.PassportValueRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.util.StringUtils.isEmpty;

public class PassportPdfFileGenerator implements FileGenerator {

    private PassportValueRepository passportValueRepository;
    private Integer versionId;
    private String head;
    private String code;
    private String category;

    public PassportPdfFileGenerator(PassportValueRepository passportValueRepository, Integer versionId,
                                    String head, String code) {
        this.passportValueRepository = passportValueRepository;
        this.versionId = versionId;
        this.head = head;
        this.code = code;
    }

    public PassportPdfFileGenerator(PassportValueRepository passportValueRepository, Integer versionId,
                                    String head, String code, String category) {
        this.passportValueRepository = passportValueRepository;
        this.versionId = versionId;
        this.head = head;
        this.code = code;
        this.category = category;
    }

    @Override
    public void generate(OutputStream outputStream) {


        List<PassportValueEntity> values = passportValueRepository.findAllByVersionIdOrderByAttributePosition(versionId);
        if (isEmpty(values)) return;

        Map<String, String> passportToWrite = new LinkedHashMap<>();

        passportToWrite.put("Код справочника", code);
        if (!isEmpty(category))
            passportToWrite.put("Категория справочника", category);

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
        //not close for generator
    }
}