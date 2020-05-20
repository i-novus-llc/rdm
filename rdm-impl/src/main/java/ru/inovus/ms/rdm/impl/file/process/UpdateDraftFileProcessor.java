package ru.inovus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.data.util.Pair;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.api.service.DraftService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class UpdateDraftFileProcessor implements FileProcessor<Draft> {

    private static final String FILE_INVALID_EXCEPTION_CODE = "check.your.xml";

    private Integer refBookId;

    private DraftService draftService;

    public UpdateDraftFileProcessor(Integer refBookId, DraftService draftService) {
        this.refBookId = refBookId;
        this.draftService = draftService;
    }

    protected abstract void setFile(InputStream inputStream);

    public abstract Map<String, Object> getPassport();

    protected abstract Pair<Structure, Map<String, List<AttributeValidation>>> getStructureAndValidations();

    @Override
    public Draft process(Supplier<InputStream> fileSource) {
        try(InputStream inputStream = fileSource.get()) {
            setFile(inputStream);

            Map<String, Object> passport = getPassport();
            Pair<Structure, Map<String, List<AttributeValidation>>> pair = getStructureAndValidations();
            CreateDraftRequest request = new CreateDraftRequest(refBookId, pair.getFirst(), passport, pair.getSecond());
            return draftService.create(request);

        }  catch (IOException e) {
            XmlParseUtils.throwXmlReadError(e);

        } catch (UserException e) {
            throw e;

        } catch (Exception e) {
            throw new UserException(FILE_INVALID_EXCEPTION_CODE, e);
        }
        return null;
    }
}
