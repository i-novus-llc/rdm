package ru.i_novus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.service.RefBookService;

import java.io.InputStream;
import java.util.function.Supplier;

public abstract class CreateRefBookFileProcessor implements FileProcessor<RefBook> {

    private static final String REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE = "refbook.is.not.created";

    private final RefBookService refBookService;

    public CreateRefBookFileProcessor(RefBookService refBookService) {
        this.refBookService = refBookService;
    }

    protected abstract RefBookCreateRequest getRefBookCreateRequest();

    protected abstract void setFile(InputStream inputStream);

    @Override
    public RefBook process(Supplier<InputStream> fileSource) {

        RefBookCreateRequest refBookCreateRequest;
        try (InputStream inputStream = fileSource.get()) {
            setFile(inputStream);
            refBookCreateRequest = getRefBookCreateRequest();

        } catch (UserException e) {
            throw e;

        } catch (Exception e) {
            throw new RdmException(e);
        }

        if (refBookCreateRequest != null) {
            return refBookService.create(refBookCreateRequest);
        }

        throw new UserException(REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE);
    }
}
