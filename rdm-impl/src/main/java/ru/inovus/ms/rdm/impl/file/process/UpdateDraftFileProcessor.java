package ru.inovus.ms.rdm.impl.file.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.api.service.DraftService;

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

    public abstract Map<String, Object> getPassport();

    protected abstract Pair<Structure, Map<String, AttributeValidation>> getStructure();

    @Override
    public Draft process(Supplier<InputStream> fileSource) {
        try(InputStream inputStream = fileSource.get()) {
            setFile(inputStream);

            Map<String, Object> passport = getPassport();
            Pair<Structure, Map<String, AttributeValidation>> structure = getStructure();
            return draftService.create(new CreateDraftRequest(refBookId, structure.getFirst(), passport, structure.getSecond()));

        }  catch (IOException e) {
            logger.error("cannot get inputStream", e);
        }
        return null;
    }
}
