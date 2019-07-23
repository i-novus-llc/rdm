package ru.inovus.ms.rdm.file.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.model.draft.Draft;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.DraftService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public abstract class UpdateDraftFileProcessor implements FileProcessor<Draft> {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDraftFileProcessor.class);

    private Integer refBookId;

    private DraftService draftService;

    public UpdateDraftFileProcessor(Integer refBookId, DraftService draftService) {
        this.refBookId = refBookId;
        this.draftService = draftService;
    }

    protected abstract void setFile(InputStream inputStream);

    public abstract Map<String, String> getPassport();

    protected abstract Structure getStructure();

    @Override
    public Draft process(Supplier<InputStream> fileSource) {
        try(InputStream inputStream = fileSource.get()) {
            setFile(inputStream);

            Map<String, String> passport = getPassport();
            Structure structure = getStructure();
            return draftService.create(new CreateDraftRequest(refBookId, structure, passport));

        }  catch (IOException e) {
            logger.error("cannot get inputStream", e);
        }
        return null;
    }
}
