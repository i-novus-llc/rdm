package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.exception.FileExtensionException;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.RefBookDataService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.impl.file.FileStorage;
import ru.inovus.ms.rdm.impl.file.process.XmlCreateRefBookFileProcessor;
import ru.inovus.ms.rdm.impl.util.FileUtil;

import java.io.InputStream;
import java.util.function.Supplier;

@Primary
@Service
public class RefBookDataServiceImpl implements RefBookDataService {

    private static final String REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE = "refbook.is.not.created";
    private static final String REFBOOK_IS_NOT_CREATED_FROM_XLSX_EXCEPTION_CODE = "refbook.is.not.created.from.xlsx";

    private RefBookService refBookService;

    private DraftService draftService;

    private FileStorage fileStorage;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public RefBookDataServiceImpl(RefBookService refBookService,
                                  DraftService draftService,
                                  FileStorage fileStorage) {
        this.refBookService = refBookService;

        this.draftService = draftService;

        this.fileStorage = fileStorage;
    }

    @Override
    public Draft create(FileModel fileModel) {

        switch (FileUtil.getExtension(fileModel.getName())) {
            case "XLSX": return createByXlsx(fileModel);
            case "XML": return createByXml(fileModel);
            default: throw new FileExtensionException();
        }
    }

    @SuppressWarnings("unused")
    private Draft createByXlsx(FileModel fileModel) {
        throw new UserException(REFBOOK_IS_NOT_CREATED_FROM_XLSX_EXCEPTION_CODE);
    }

    private Draft createByXml(FileModel fileModel) {

        RefBook refBook;
        try (XmlCreateRefBookFileProcessor createRefBookFileProcessor = new XmlCreateRefBookFileProcessor(refBookService)) {
            Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());
            refBook = createRefBookFileProcessor.process(inputStreamSupplier);
        }

        if (refBook == null)
            throw new UserException(REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE);

        try {
            return draftService.create(refBook.getRefBookId(), fileModel);

        } catch (Exception e) {
            refBookService.delete(refBook.getRefBookId());

            throw e;
        }
    }
}
