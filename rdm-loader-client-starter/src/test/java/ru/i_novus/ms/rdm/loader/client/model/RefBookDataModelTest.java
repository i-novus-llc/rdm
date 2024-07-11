package ru.i_novus.ms.rdm.loader.client.model;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum;
import ru.i_novus.ms.rdm.loader.client.BaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.CREATE_ONLY;

public class RefBookDataModelTest extends BaseTest {

    @Test
    public void testClass() {

        final RefBookDataModel emptyModel = new RefBookDataModel();
        assertSpecialEquals(emptyModel);
        assertObjects(Assert::assertEquals, emptyModel, emptyModel);
        testEquals(copy(emptyModel), emptyModel);

        final RefBookDataModel dataModel = build("change_set_id_test", CREATE_ONLY,
                "test", "Тест", "structure", "data");
        assertObjects(Assert::assertNotEquals,emptyModel, dataModel);
        testEquals(copy(dataModel), dataModel);

        final RefBookDataModel fileModel = new RefBookDataModel(new ClassPathResource("dir/file.ext"));
        assertObjects(Assert::assertNotEquals, dataModel, fileModel);
        testEquals(copy(fileModel), fileModel);
    }

    private void testEquals(RefBookDataModel current, RefBookDataModel actual) {

        assertObjects(Assert::assertEquals, current, actual);

        assertEquals(current.getChangeSetId(), actual.getChangeSetId());
        assertEquals(current.getUpdateType(), actual.getUpdateType());
        assertEquals(current.getCode(), actual.getCode());

        assertEquals(current.getName(), actual.getName());
        assertEquals(current.getStructure(), actual.getStructure());
        assertEquals(current.getData(), actual.getData());
        assertEquals(current.getFile(), actual.getFile());
    }

    @SuppressWarnings("SameParameterValue")
    private RefBookDataModel build(String changeSetId, RefBookDataUpdateTypeEnum updateType,
                                   String code, String name, String structure, String data) {

        final RefBookDataModel result = new RefBookDataModel();
        result.setChangeSetId(changeSetId);
        result.setUpdateType(updateType);
        result.setCode(code);

        result.setName(name);
        result.setStructure(structure);
        result.setData(data);

        return result;
    }

    private RefBookDataModel copy(RefBookDataModel origin) {
        
        final RefBookDataModel result = new RefBookDataModel();
        result.setChangeSetId(origin.getChangeSetId());
        result.setUpdateType(origin.getUpdateType());
        result.setCode(origin.getCode());

        result.setName(origin.getName());
        result.setStructure(origin.getStructure());
        result.setData(origin.getData());
        result.setFile(origin.getFile());

        return result;
    }
}