package ru.inovus.ms.rdm.file;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import ru.inovus.ms.rdm.model.RefBookCreateRequest;
import ru.inovus.ms.rdm.service.api.RefBookService;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class XmlCreateRefBookFileProcessorTest {

    private final static String XML_FILE = "/file/uploadFile.xml";

    @Mock
    RefBookService refBookService;

    @Before
    public void setUp() throws Exception {
        reset(refBookService);
    }

    @Test
    public void testProcess() {
        try(XmlCreateRefBookFileProcessor createRefBookFileProcessor = new XmlCreateRefBookFileProcessor(refBookService)) {
            createRefBookFileProcessor.process(() -> getClass().getResourceAsStream(XML_FILE));
            ArgumentCaptor<RefBookCreateRequest> argumentCaptor = ArgumentCaptor.forClass(RefBookCreateRequest.class);
            verify(refBookService, times(1)).create(argumentCaptor.capture());
            Assert.assertEquals("TEST_REF_CODE", argumentCaptor.getValue().getCode());
        }
    }
}
