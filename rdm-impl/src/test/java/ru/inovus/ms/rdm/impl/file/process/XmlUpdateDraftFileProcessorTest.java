package ru.inovus.ms.rdm.impl.file.process;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.inovus.ms.rdm.api.model.validation.*;
import ru.inovus.ms.rdm.impl.file.UploadFileTestData;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.service.DraftService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        CreateDraftRequest expected = new CreateDraftRequest(TEST_REF_BOOK_ID,
                UploadFileTestData.createStructure(), UploadFileTestData.createPassport(),
                Map.of(
                        "string", List.of(new PlainSizeAttributeValidation(40), new RegExpAttributeValidation("[а-яА-я]*")),
                        "integer", List.of(new IntRangeAttributeValidation(BigInteger.ONE, BigInteger.TEN), new UniqueAttributeValidation()),
                        "date", List.of(new DateRangeAttributeValidation(LocalDate.of(2019, 02, 01), LocalDate.of(2019, 02, 02))),
                        "boolean", List.of(new RequiredAttributeValidation()),
                        "float", List.of(new FloatSizeAttributeValidation(2, 2), new FloatRangeAttributeValidation(BigDecimal.valueOf(2.43), BigDecimal.valueOf(10.12)))
                )
        );
        expected.setReferrerValidationRequired(true);

        try(XmlUpdateDraftFileProcessor fileProcessor = new XmlUpdateDraftFileProcessor(TEST_REF_BOOK_ID, draftService)) {
            fileProcessor.process(() -> getClass().getResourceAsStream(XML_FILE));
            ArgumentCaptor<CreateDraftRequest> captor = ArgumentCaptor.forClass(CreateDraftRequest.class);
            verify(draftService, times(1)).create(captor.capture());
            Assert.assertEquals(expected, captor.getValue());
        }
    }
}
