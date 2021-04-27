package ru.i_novus.ms.rdm.n2o.provider;

import ru.i_novus.ms.rdm.api.model.Structure;

import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.DEFAULT_STRUCTURE;

public abstract class BaseRecordProviderTest {

    protected static final String TEST_ACTION = "trial";

    protected static final int TEST_REFBOOK_DRAFT_ID = -100;

    protected static final String CONTEXT_FORMAT = "%s_%s";
    protected static final String TEST_CONTEXT = String.format(CONTEXT_FORMAT, TEST_REFBOOK_DRAFT_ID, TEST_ACTION);

    public void testRead() {

        testRead(null);
        testRead(Structure.EMPTY);
        testRead(createStructure());
    }

    abstract void testRead(Structure structure);

    static Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }
}
