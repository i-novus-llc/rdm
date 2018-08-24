package ru.inovus.ms.rdm.file.export;

import com.itextpdf.text.DocumentException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.PassportAttributeValue;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

        Map<String, String> passportToWrite = new LinkedHashMap<>();
        version.getPassport().entrySet().stream()
                .forEach(entry -> passportToWrite.put(String.valueOf(entry.getValue().getName()), entry.getValue().getValue()));
        String paragraph = Optional.ofNullable(version.getPassport().get(head)).map(PassportAttributeValue::getValue).orElse("");



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