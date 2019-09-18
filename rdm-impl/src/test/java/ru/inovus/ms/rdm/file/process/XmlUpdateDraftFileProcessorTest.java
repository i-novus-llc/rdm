package ru.inovus.ms.rdm.file.process;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.inovus.ms.rdm.file.UploadFileTestData;
import ru.inovus.ms.rdm.n2o.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.n2o.service.api.DraftService;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class XmlUpdateDraftFileProcessorTest {

    private final Integer TEST_REF_BOOK_ID = 1;

    private final static String XML_FILE = "/file/uploadFile.xml";

    @Mock
    private DraftService draftService;

    @Before
    public void setUp() {
        reset(draftService);
    }

    @Test
    public void testProcess() {
        CreateDraftRequest expected = new CreateDraftRequest(TEST_REF_BOOK_ID, UploadFileTestData.createStructure(), UploadFileTestData.createPassport());

        try(XmlUpdateDraftFileProcessor fileProcessor = new XmlUpdateDraftFileProcessor(TEST_REF_BOOK_ID, draftService)) {
            fileProcessor.process(() -> getClass().getResourceAsStream(XML_FILE));
            ArgumentCaptor<CreateDraftRequest> captor = ArgumentCaptor.forClass(CreateDraftRequest.class);
            verify(draftService, times(1)).create(captor.capture());
            Assert.assertEquals(expected, captor.getValue());
        }
    }
}
