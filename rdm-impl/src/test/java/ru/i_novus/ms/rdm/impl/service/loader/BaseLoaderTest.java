package ru.i_novus.ms.rdm.impl.service.loader;

import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.impl.BaseTest;

import java.util.HashMap;

import static java.util.Collections.emptyMap;

public class BaseLoaderTest extends BaseTest {

    protected static final int REFBOOK_ID = 1;
    protected static final int DRAFT_ID = 2;

    protected static final String LOADED_CODE = "LOADED_DATA_";
    protected static final String LOADED_NAME = "Loaded Name ";
    protected static final String LOADED_STRUCTURE = "{}";
    protected static final String LOADED_DATA = "{}";

    protected static final String LOADED_FILE_NAME = "loadedData_";
    protected static final String LOADED_FILE_EXT = ".xml";
    protected static final String LOADED_FILE_FOLDER = "src/test/resources/" + "testLoader/";

    protected RefBook createRefBook(int index) {

        final RefBook result = new RefBook();
        result.setRefBookId(index);
        result.setCode(LOADED_CODE + index);

        result.setPassport(new HashMap<>(1));
        result.getPassport().put("name", LOADED_NAME + index);

        return result;
    }

    protected Draft createDraft(int index) {

        final Draft result = new Draft();
        result.setId(index);

        return result;
    }

    protected RefBookDataRequest createJsonDataRequest(int index) {

        final RefBookDataRequest result = new RefBookDataRequest();
        result.setCode(LOADED_CODE + index);

        result.setPassport(new HashMap<>(1));
        result.getPassport().put("name", LOADED_NAME + index);

        result.setStructure(LOADED_STRUCTURE);
        result.setData(LOADED_DATA);

        return result;
    }

    protected RefBookDataRequest createFileDataRequest(int index) {

        final RefBookDataRequest result = new RefBookDataRequest();
        result.setCode(LOADED_CODE + index);
        result.setPassport(emptyMap());

        final String fileName = getFileName(index);
        final FileModel fileModel = new FileModel(null, fileName);
        result.setFileModel(fileModel);

        return result;
    }

    protected String getFileName(int index) {

        return String.format("%s%d%s", LOADED_FILE_NAME, index, LOADED_FILE_EXT);
    }
}
