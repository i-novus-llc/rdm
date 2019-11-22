package ru.inovus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.inovus.ms.rdm.api.service.RefBookService;

import java.io.InputStream;
import java.util.function.Supplier;

public abstract class CreateRefBookFileProcessor implements FileProcessor<RefBook> {

    private static final Logger logger = LoggerFactory.getLogger(CreateRefBookFileProcessor.class);

    private RefBookService refBookService;

    public CreateRefBookFileProcessor(RefBookService refBookService) {
        this.refBookService = refBookService;
    }

    protected abstract RefBookCreateRequest getRefBookCreateRequest();

    protected abstract void setFile(InputStream inputStream);

    @Override
    public RefBook process(Supplier<InputStream> fileSource) {
        try(InputStream inputStream = fileSource.get()) {
            setFile(inputStream);
            RefBookCreateRequest refBookCreateRequest = getRefBookCreateRequest();
            if (refBookCreateRequest == null) {
                return null;
            }
            return refBookService.create(refBookCreateRequest);
        } catch (Exception e) {
            logger.error("cannot process file", e);
            if (e instanceof UserException) {
                throw (UserException) e;
            } else
                throw new RdmException(e);
        }
    }
}
