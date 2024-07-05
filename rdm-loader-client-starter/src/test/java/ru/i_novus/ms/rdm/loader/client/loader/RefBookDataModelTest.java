package ru.i_novus.ms.rdm.loader.client.loader;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ru.i_novus.ms.rdm.loader.client.loader.model.RefBookDataModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RefBookDataModelTest extends BaseTest {

    @Test
    public void testClass() {

        RefBookDataModel emptyModel = new RefBookDataModel();
        assertSpecialEquals(emptyModel);
        assertObjects(Assert::assertEquals, emptyModel, emptyModel);
        testEquals(copy(emptyModel), emptyModel);

        RefBookDataModel dataModel = build("test", "Тест", "structure", "data");
        assertObjects(Assert::assertNotEquals,emptyModel, dataModel);
        testEquals(copy(dataModel), dataModel);

        RefBookDataModel fileModel = new RefBookDataModel(new ClassPathResource("dir/file.ext"));
        assertObjects(Assert::assertNotEquals, dataModel, fileModel);
        testEquals(copy(fileModel), fileModel);
    }

    private void testEquals(RefBookDataModel current, RefBookDataModel actual) {

        assertObjects(Assert::assertEquals, current, actual);

        assertEquals(current.getCode(), actual.getCode());
        assertEquals(current.getName(), actual.getName());
        assertEquals(current.getStructure(), actual.getStructure());
        assertEquals(current.getData(), actual.getData());
        assertEquals(current.getFile(), actual.getFile());
    }

    private RefBookDataModel build(String code, String name, String structure, String data) {

        final RefBookDataModel result = new RefBookDataModel();
        result.setCode(code);
        result.setName(name);
        result.setStructure(structure);
        result.setData(data);

        return result;
    }

    private RefBookDataModel copy(RefBookDataModel origin) {
        
        final RefBookDataModel result = new RefBookDataModel();
        result.setCode(origin.getCode());
        result.setName(origin.getName());
        result.setStructure(origin.getStructure());
        result.setData(origin.getData());
        result.setFile(origin.getFile());

        return result;
    }
}