package ru.i_novus.ms.rdm.loader.client;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ru.i_novus.ms.rdm.test.BaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RefBookDataModelTest extends BaseTest {

    @Test
    public void testClass() {

        RefBookDataModel emptyModel = new RefBookDataModel();
        assertSpecialEquals(emptyModel);
        assertObjects(Assert::assertEquals, emptyModel, emptyModel);
        testEquals(copy(emptyModel), emptyModel);

        RefBookDataModel jsonModel = new RefBookDataModel("test", "Тест", "structure", "data");
        assertObjects(Assert::assertNotEquals,emptyModel, jsonModel);
        testEquals(copy(jsonModel), jsonModel);

        RefBookDataModel fileModel = new RefBookDataModel(null, null, null, new ClassPathResource("dir/file.ext"));
        assertObjects(Assert::assertNotEquals, jsonModel, fileModel);
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

    private RefBookDataModel copy(RefBookDataModel origin) {
        
        RefBookDataModel result = new RefBookDataModel();
        result.setCode(origin.getCode());
        result.setName(origin.getName());
        result.setStructure(origin.getStructure());
        result.setData(origin.getData());
        result.setFile(origin.getFile());

        return result;
    }
}