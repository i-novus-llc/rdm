package ru.inovus.ms.rdm.esnsi;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import ru.inovus.ms.rdm.esnsi.api.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Void;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.reflection.FieldSetter.setField;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class EsnsiSyncTest {

    private static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    @Mock
    private EsnsiIntegrationService integrationService;

    @Mock
    private EsnsiSmevClient esnsiSmevClient;

    @Mock
    private EsnsiIntegrationDao dao;

    private ObjectFactory objectFactory = new ObjectFactory();

    private DatatypeFactory dtf;

    {
        try {
            dtf = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            fail();
        }
    }

    @Test
    public void main() throws Exception {
        String dictCode = "01-519";
        int lastRevision = 3;
        String getRevisionListMsgId = "1";
        String getStructureMsgId = "2";
        String getDataMsgId = "3";

        setField(integrationService, Objects.requireNonNull(getField(EsnsiIntegrationService.class, "codes")), List.of(dictCode));
        setField(integrationService, Objects.requireNonNull(getField(EsnsiIntegrationService.class, "objectFactory")), objectFactory);
        setField(integrationService, Objects.requireNonNull(getField(EsnsiIntegrationService.class, "esnsiClient")), esnsiSmevClient);
        setField(integrationService, Objects.requireNonNull(getField(EsnsiIntegrationService.class, "dao")), dao);

        when(esnsiSmevClient.sendRequest(any(GetClassifierRevisionListRequestType.class), any())).then(invocation -> {
            AcceptRequestDocument getRevisionListAcceptRequest = objectFactory.createAcceptRequestDocument();
            getRevisionListAcceptRequest.setMessageId(getRevisionListMsgId);
            return getRevisionListAcceptRequest;
        });

        when(esnsiSmevClient.getResponse(any(), GetClassifierRevisionListResponseType.class)).then(invocation -> {
            GetClassifierRevisionListResponseType revisionList = objectFactory.createGetClassifierRevisionListResponseType();
            GetClassifierRevisionListResponseType.RevisionDescriptor revision1 = objectFactory.createGetClassifierRevisionListResponseTypeRevisionDescriptor();
            GetClassifierRevisionListResponseType.RevisionDescriptor revision2 = objectFactory.createGetClassifierRevisionListResponseTypeRevisionDescriptor();
            GetClassifierRevisionListResponseType.RevisionDescriptor revision3 = objectFactory.createGetClassifierRevisionListResponseTypeRevisionDescriptor();
            revision1.setRevision(1);
            revision2.setRevision(2);
            revision3.setRevision(lastRevision);
            revision1.setTimestamp(dtf.newXMLGregorianCalendarDate(2019, 8, 1, 1));
            revision2.setTimestamp(dtf.newXMLGregorianCalendarDate(2020, 8, 1, 1));
            revision3.setTimestamp(dtf.newXMLGregorianCalendarDate(2021, 8, 1, 1));
            revisionList.getRevisionDescriptor().addAll(
                List.of(
                    revision1,
                    revision2,
                    revision3
                )
            );
            return Map.entry(revisionList, EMPTY_INPUT_STREAM);
        });

        when(dao.getLastVersionRevisionAndCreateNewIfNecessary(any())).thenReturn(1);

        when(esnsiSmevClient.sendRequest(any(GetClassifierStructureRequestType.class), any())).then(invocation -> {
            AcceptRequestDocument getStructureAcceptRequest = objectFactory.createAcceptRequestDocument();
            getStructureAcceptRequest.setMessageId(getStructureMsgId);
            return getStructureAcceptRequest;
        });

        ClassifierDescriptorListType descriptor = objectFactory.createClassifierDescriptorListType();
        descriptor.setCode(dictCode);
        descriptor.setPublicId(dictCode);
        descriptor.setRevision(lastRevision);
        descriptor.setName("Общероссийский классификатор объектов административно-территориального деления ОКАТО");

        GetClassifierStructureResponseType struct = objectFactory.createGetClassifierStructureResponseType();
        when(esnsiSmevClient.getResponse(any(), GetClassifierStructureResponseType.class)).then(invocation -> {
            struct.setClassifierDescriptor(descriptor);
            setField(struct, Objects.requireNonNull(getField(struct.getClass(), "attributeList")), struct_01_519());
            return Map.entry(struct, EMPTY_INPUT_STREAM);
        });

        when(esnsiSmevClient.sendRequest(any(GetClassifierDataRequestType.class), any())).then(invocation -> {
            AcceptRequestDocument acceptRequestDocument = objectFactory.createAcceptRequestDocument();
            acceptRequestDocument.setMessageId(getDataMsgId);
            return acceptRequestDocument;
        });

        when(esnsiSmevClient.getResponse(any(), GetClassifierDataResponseType.class)).then(invocation -> {
            GetClassifierDataResponseType data = objectFactory.createGetClassifierDataResponseType();
            data.setClassifierDescriptor(descriptor);
            return Map.entry(data, Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "01-519-test.zip"
            )));
        });

        doAnswer(invocation -> {
            Consumer<String[]> consumer = invocation.getArgument(0);
            consumer.accept(new String[] {"01", "000", "000", "000", "1", "Алтайский край", "г Барнаул", "", "000", "0", "31.12.1996", "31.12.1996"});
            consumer.accept(new String[] {"01", "200", "000", "000", "1", "Районы Алтайского края/", "", "", "000", "0", "31.12.1996", "31.12.1996"});
            consumer.accept(new String[] {"01", "201", "000", "000", "1", "Алейский район", "г Алейск", "", "000", "0", "31.12.1996", "31.12.1996"});
            return Void.TYPE;
        }).when(dao).readRows(any(), anyString(), anyInt());
        doCallRealMethod().when(integrationService).update();
        integrationService.update();
//        verify(dao, times(1)).insert(anyList(), refEq(struct));
        Diff build = DiffBuilder.compare(Input.fromStream(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "to-rdm.xml"
        )))).withTest(Input.fromFile(dictCode + "-" + lastRevision + ".xml")).ignoreWhitespace().build();
        Files.delete(Path.of(dictCode + "-" + lastRevision + ".xml"));
        assertFalse(build.hasDifferences());
    }

    private List<ClassifierAttribute> struct_01_519() {
        ClassifierAttribute field_1 = objectFactory.createClassifierAttribute();
        field_1.setUid("9a113690-1e16-43a2-b4c6-1d958be348de");
        field_1.setType(AttributeType.STRING);
        field_1.setName("Код территории");
        field_1.setLength(255);
        field_1.setRequired(true);

        ClassifierAttribute field_2 = objectFactory.createClassifierAttribute();
        field_2.setUid("59822964-a3cb-42ca-a7a3-efecc8ed46f7");
        field_2.setType(AttributeType.STRING);
        field_2.setName("Код района/города");
        field_2.setLength(255);
        field_2.setRequired(false);

        ClassifierAttribute field_3 = objectFactory.createClassifierAttribute();
        field_3.setUid("82a8a284-cfd3-4089-9a05-03f2ec835942");
        field_3.setType(AttributeType.STRING);
        field_3.setName("Код РП/сельсовета");
        field_3.setLength(255);
        field_3.setRequired(false);

        ClassifierAttribute field_4 = objectFactory.createClassifierAttribute();
        field_4.setUid("8ac6ff86-3759-4be0-b278-153f8701884c");
        field_4.setType(AttributeType.STRING);
        field_4.setName("Код сельского населенного пункта");
        field_4.setLength(255);
        field_4.setRequired(false);

        ClassifierAttribute field_5 = objectFactory.createClassifierAttribute();
        field_5.setUid("6b8c4d32-31ae-40a7-b773-a90620a6ee38");
        field_5.setType(AttributeType.STRING);
        field_5.setName("Код раздела");
        field_5.setLength(255);
        field_5.setRequired(false);

        ClassifierAttribute field_6 = objectFactory.createClassifierAttribute();
        field_6.setUid("e59728ba-f49d-493d-afcf-0aa1e3e43708");
        field_6.setType(AttributeType.STRING);
        field_6.setName("Наименование");
        field_6.setLength(255);
        field_6.setRequired(false);

        ClassifierAttribute field_7 = objectFactory.createClassifierAttribute();
        field_7.setUid("e87afb9c-de94-40b7-8edb-1200e4acd279");
        field_7.setType(AttributeType.STRING);
        field_7.setName("Дополнительные данные");
        field_7.setLength(255);
        field_7.setRequired(false);

        ClassifierAttribute field_8 = objectFactory.createClassifierAttribute();
        field_8.setUid("3244cc1c-1383-4bb0-87e8-33ca872bb091");
        field_8.setType(AttributeType.TEXT);
        field_8.setName("Описание (пояснение)");
        field_8.setRequired(false);

        ClassifierAttribute field_9 = objectFactory.createClassifierAttribute();
        field_9.setUid("bd80b024-97e1-4035-99e8-38d9e158e28d");
        field_9.setType(AttributeType.STRING);
        field_9.setName("Номер последнего изменения");
        field_9.setLength(255);
        field_9.setRequired(false);

        ClassifierAttribute field_10 = objectFactory.createClassifierAttribute();
        field_10.setUid("aba4115d-288e-4e1c-9e7f-51ea9066d5d0");
        field_10.setType(AttributeType.STRING);
        field_10.setName("Тип последнего изменения");
        field_10.setLength(255);
        field_10.setRequired(false);

        ClassifierAttribute field_11 = objectFactory.createClassifierAttribute();
        field_11.setUid("276c3348-a647-4850-9a8d-354fb5a727d4");
        field_11.setType(AttributeType.DATE);
        field_11.setName("Дата принятия изменения");
        field_11.setDateStartRange(dtf.newXMLGregorianCalendarDate(1996, 12, 31, 1));
        field_11.setDateEndRange(dtf.newXMLGregorianCalendarDate(1996, 12, 31, 1));
        field_11.setLength(255);
        field_11.setRequired(false);

        ClassifierAttribute field_12 = objectFactory.createClassifierAttribute();
        field_12.setUid("0b69adcd-0bc7-4bae-bb72-5668180db3d5");
        field_12.setType(AttributeType.DATE);
        field_12.setName("Дата введения изменения");
        field_12.setDateStartRange(dtf.newXMLGregorianCalendarDate(1996, 12, 31, 1));
        field_12.setDateEndRange(dtf.newXMLGregorianCalendarDate(1996, 12, 31, 1));
        field_12.setLength(255);
        field_12.setRequired(false);

        List<ClassifierAttribute> list = List.of(
                field_1, field_2, field_3, field_4, field_5, field_6,
                field_7, field_8, field_9, field_10, field_11, field_12
        );
        for (int i = 0; i < list.size(); i++)
            list.get(i).setOrder(i + 1);
        return list;
    }

    private Field getField(Class c, String name) {
        try {
            return c.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            fail();
            return null;
        }
    }

}
