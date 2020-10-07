package ru.i_novus.ms.rdm.n2o.provider;

import ru.i_novus.ms.rdm.api.model.Structure;

import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.ATTRIBUTE_LIST;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.REFERENCE_LIST;

public abstract class BaseRecordProviderTest {

    protected static final String TEST_ACTION = "trial";

    protected static final int TEST_REFBOOK_DRAFT_ID = -100;

    protected static final String CONTEXT_FORMAT = "%s_%s";
    protected static final String TEST_CONTEXT = String.format(CONTEXT_FORMAT, TEST_REFBOOK_DRAFT_ID, TEST_ACTION);

    /** Создание структуры с глубоким копированием атрибутов и ссылок. */
    static Structure createStructure() {

        Structure structure = new Structure(ATTRIBUTE_LIST, REFERENCE_LIST);
        return new Structure(structure);
    }

    public void testRead() {

        testRead(null);
        testRead(Structure.EMPTY);
        testRead(createStructure());
    }

    abstract void testRead(Structure structure);
}
