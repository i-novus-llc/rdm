package ru.inovus.ms.rdm.file;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.inovus.ms.rdm.model.CreateDraftRequest;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.DraftService;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static ru.inovus.ms.rdm.file.export.XmlFileGenerateProcessTest.createFullTestStructure;

@RunWith(MockitoJUnitRunner.class)
public class XmlUpdateDraftFileProcessorTest {

    private final Integer TEST_REF_BOOK_ID = 1;

    private final static String XML_FILE = "/file/uploadFile.xml";

    @Mock
    private DraftService draftService;


    @Before
    public void setUp() throws Exception {
        reset(draftService);
    }

    @Test
    public void testProcess() throws Exception {
        Structure expectedStructure = createFullTestStructure();
        //убрать когда переделаем ссылочность
        expectedStructure.setReferences(new ArrayList<>());
        CreateDraftRequest expected = new CreateDraftRequest(TEST_REF_BOOK_ID, expectedStructure, null);

        try(XmlUpdateDraftFileProcessor fileProcessor = new XmlUpdateDraftFileProcessor(TEST_REF_BOOK_ID, draftService)) {
            fileProcessor.process(() -> getClass().getResourceAsStream(XML_FILE));
            ArgumentCaptor<CreateDraftRequest> captor = ArgumentCaptor.forClass(CreateDraftRequest.class);
            verify(draftService, times(1)).create(captor.capture());
            Assert.assertEquals(expected, captor.getValue());
        }

    }
}
