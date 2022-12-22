package ru.i_novus.ms.rdm.impl.entity;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.impl.BaseTest;

public class RefBookDetailModelTest extends BaseTest {

    @Test
    public void testClass() {

        RefBookDetailModel data = new RefBookDetailModel();
        assertSpecialEquals(data);

        RefBookDetailModel modelData = createModelData();
        assertObjects(Assert::assertNotEquals, data, modelData);

        RefBookDetailModel copyData = copyModelData(modelData);
        assertObjects(Assert::assertEquals, modelData, copyData);
    }

    private RefBookDetailModel createModelData() {

        RefBookDetailModel data = new RefBookDetailModel();

        data.setCurrentVersionId(1);
        data.setDraftVersion(new RefBookVersionEntity());
        data.setLastPublishedVersion(new RefBookVersionEntity());

        data.setRemovable(true);
        data.setHasReferrer(false);

        data.setHasDataConflict(true);
        data.setHasUpdatedConflict(true);
        data.setHasAlteredConflict(false);
        data.setHasStructureConflict(false);
        data.setLastHasConflict(false);

        return data;
    }

    private RefBookDetailModel copyModelData(RefBookDetailModel origin) {

        RefBookDetailModel data = new RefBookDetailModel();

        data.setCurrentVersionId(origin.getCurrentVersionId());
        data.setDraftVersion(origin.getDraftVersion());
        data.setLastPublishedVersion(origin.getLastPublishedVersion());

        data.setRemovable(origin.getRemovable());
        data.setHasReferrer(origin.getHasReferrer());

        data.setHasDataConflict(origin.getHasDataConflict());
        data.setHasUpdatedConflict(origin.getHasUpdatedConflict());
        data.setHasAlteredConflict(origin.getHasAlteredConflict());
        data.setHasStructureConflict(origin.getHasStructureConflict());
        data.setLastHasConflict(origin.getLastHasConflict());

        return data;
    }
}