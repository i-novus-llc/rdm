package ru.i_novus.ms.rdm.impl.file.process;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.service.RefBookService;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class XmlCreateRefBookFileProcessorTest {

    private final static String XML_FILE = "/file/uploadFile.xml";

    @Mock
    RefBookService refBookService;

    @Before
    public void setUp() {
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
