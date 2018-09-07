package ru.inovus.ms.rdm.file.export;

import com.itextpdf.text.DocumentException;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.repositiory.PassportValueRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.util.CollectionUtils.isEmpty;

public class PassportPdfFileGenerator implements FileGenerator {

    PassportValueRepository passportValueRepository;
    Integer versionId;
    String head;

    public PassportPdfFileGenerator(PassportValueRepository passportValueRepository, Integer versionId, String head) {
        this.passportValueRepository = passportValueRepository;
        this.versionId = versionId;
        this.head = head;
    }

    @Override
    public void generate(OutputStream outputStream) {


        List<PassportValueEntity> values = passportValueRepository.findAllByVersionIdOrderByAttributePosition(versionId);
        if (isEmpty(values)) return;

        Map<String, String> passportToWrite = new LinkedHashMap<>();

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