package ru.i_novus.ms.rdm.impl.util;

import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookDetailModel;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Created by znurgaliev on 09.08.2018.
 */
public class ModelGenerator {

    private ModelGenerator() {
        // Nothing to do.
    }

    public static RefBook refBookModel(
            RefBookVersionEntity entity,
            RefBookDetailModel detailModel,
            boolean excludeDraft
    ) {
        if (entity == null) return null;

        final RefBook model = new RefBook(versionModel(entity));

        if (entity.getRefBookOperation() != null) {
            model.setCurrentOperation(entity.getRefBookOperation().getOperation());
        }

        final Structure structure = entity.getStructure();
        List<Structure.Attribute> primaries = (structure != null) ? structure.getPrimaries() : emptyList();
        model.setHasPrimaryAttribute(!primaries.isEmpty());

        return fillRefBookModel(model, detailModel, excludeDraft);
    }

    public static RefBook fillRefBookModel(
            RefBook model,
            RefBookDetailModel detailModel,
            boolean excludeDraft
    ) {

        if (model == null || detailModel == null) return model;

        if (!excludeDraft) {
            final RefBookVersionEntity draftVersion = detailModel.getDraftVersion();
            if (draftVersion != null) {
                model.setDraftVersionId(draftVersion.getId());
            }
        }

        final RefBookVersionEntity lastPublishedVersion = detailModel.getLastPublishedVersion();
        if (lastPublishedVersion != null) {
            model.setLastPublishedVersionId(lastPublishedVersion.getId());
            model.setLastPublishedVersion(lastPublishedVersion.getVersion());
            model.setLastPublishedDate(lastPublishedVersion.getFromDate());
        }

        final boolean hasReferrer = Boolean.TRUE.equals(detailModel.getHasReferrer());
        model.setRemovable(Boolean.TRUE.equals(detailModel.getRemovable()) && !hasReferrer);
        model.setHasReferrer(hasReferrer);

        model.setHasDataConflict(detailModel.getHasDataConflict());
        model.setHasUpdatedConflict(detailModel.getHasUpdatedConflict());
        model.setHasAlteredConflict(detailModel.getHasAlteredConflict());
        model.setHasStructureConflict(detailModel.getHasStructureConflict());
        model.setLastHasConflict(detailModel.getLastHasConflict());

        return model;
    }

    public static RefBookVersion versionModel(RefBookVersionEntity entity) {

        if (entity == null) return null;

        final RefBookVersion model = new RefBookVersion();
        model.setId(entity.getId());
        model.setRefBookId(entity.getRefBook().getId());
        model.setCode(entity.getRefBook().getCode());
        model.setType(entity.getRefBook().getType());
        model.setCategory(entity.getRefBook().getCategory());
        model.setVersion(entity.getVersion());
        model.setComment(entity.getComment());

        model.setFromDate(entity.getFromDate());
        model.setToDate(entity.getToDate());
        model.setStatus(entity.getStatus());
        model.setArchived(entity.getRefBook().getArchived());

        if (entity.getPassportValues() != null) {
            model.setPassport(entity.toPassport());
        }

        model.setStructure(entity.getStructure());

        model.setEditDate(entity.getLastActionDate());
        model.setOptLockValue(entity.getOptLockValue());

        return model;
    }

    public static AsyncOperationLogEntry asyncOperationLogEntryModel(AsyncOperationLogEntryEntity entity) {

        if (entity == null) return null;

        final AsyncOperationLogEntry model = new AsyncOperationLogEntry();
        model.setId(entity.getUuid());
        model.setOperationType(entity.getOperationType());
        model.setCode(entity.getCode());

        model.setStatus(entity.getStatus());
        model.setTsStart(entity.getTsStart());
        model.setTsEnd(entity.getTsEnd());

        model.setPayload(entity.getPayload());
        model.setResult(entity.getResult());
        model.setError(entity.getError());
        model.setStackTrace(entity.getStackTrace());

        return model;
    }
}
