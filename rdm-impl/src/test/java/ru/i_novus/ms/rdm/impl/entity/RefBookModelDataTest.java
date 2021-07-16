package ru.i_novus.ms.rdm.impl.entity;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.impl.BaseTest;

public class RefBookModelDataTest extends BaseTest {

    @Test
    public void testClass() {

        RefBookModelData data = new RefBookModelData();
        assertSpecialEquals(data);

        RefBookModelData modelData = createModelData();
        assertObjects(Assert::assertNotEquals, data, modelData);

        RefBookModelData copyData = copyModelData(modelData);
        assertObjects(Assert::assertEquals, modelData, copyData);
    }

    private RefBookModelData createModelData() {

        RefBookModelData data = new RefBookModelData();

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

    private RefBookModelData copyModelData(RefBookModelData origin) {

        RefBookModelData data = new RefBookModelData();

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