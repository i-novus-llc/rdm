package ru.inovus.ms.rdm.file.export;

import com.itextpdf.text.DocumentException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.PassportAttribute;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class PassportPdfFileGenerator implements FileGenerator {

    VersionService versionService;
    Integer versionId;
    String head;

    public PassportPdfFileGenerator(VersionService versionService, Integer versionId, String head) {
        this.versionService = versionService;
        this.versionId = versionId;
        this.head = head;
    }

    @Override
    public void generate(OutputStream outputStream) {


        RefBookVersion version = versionService.getById(versionId);
        if (version == null) return;

        Map<String, Object> passportToWrite = new LinkedHashMap<>();
        version.getPassport().stream()
                .forEach(attribute -> passportToWrite.put(String.valueOf(attribute.getName()), attribute.getValue()));
        String paragraph = version.getPassport().stream()
                .filter(attribute -> attribute.getCode().equals(head))
                .findFirst().map(PassportAttribute::getValue).orElse("");


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