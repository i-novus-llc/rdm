package ru.inovus.ms.rdm.esnsi.smev;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.esnsi.smev.Utils.*;

@Component
public class AdapterClient {

    @Autowired
    private AdapterConsumer adapterConsumer;

    @Autowired
    private MsgBuffer msgBuffer;

    public AcceptRequestDocument sendRequest(Object requestData, String messageId) {
        if (requestData.getClass() != CnsiRequest.class) {
            CnsiRequest cnsiRequest = OBJECT_FACTORY.createCnsiRequest();
            setRequest(cnsiRequest, requestData);
            requestData = cnsiRequest;
        }
        return adapterConsumer.sendRequest(requestData, messageId);
    }

    public <T> Map.Entry<T, InputStream> getResponse(String messageId, Class<T> responseType) {
        String msg = msgBuffer.get(messageId);
        if (msg == null)
            return null;
        ResponseDocument responseDocument;
        try {
            responseDocument = (ResponseDocument) JAXB_CTX.createUnmarshaller().unmarshal(new StringReader(msg));
        } catch (JAXBException e) {
            throw new RdmException(e);
//          Не должно выброситься, если никто не лазает в базу
        }
        if (responseDocument != null) {
            if (!responseDocument.getSenderProvidedResponseData().getRequestRejected().isEmpty()) {
                throw new RdmException(responseDocument.getSenderProvidedResponseData().getRequestRejected().stream().map(requestRejected ->
                        "[" + requestRejected.getRejectionReasonCode() + ":" + requestRejected.getRejectionReasonDescription() + "]"
                ).collect(Collectors.joining(",\n")));
            }
            try {
                return Map.entry(extractResponse(responseDocument, responseType), responseDocument.getAttachmentContentList() == null ? EMPTY_INPUT_STREAM : responseDocument.getAttachmentContentList().getAttachmentContent().iterator().next().getContent().getInputStream());
            } catch (IOException e) {
//              Не должно выброситься, никаких IO операций не производится
                throw new RdmException(e);
            }
        }
        return null;
    }

    public void acknowledge(String messageId) {
        msgBuffer.remove(messageId);
    }

    private void setRequest(CnsiRequest cnsiRequest, Object requestData) {
        Class c = requestData.getClass();
        if (c == GetAvailableIncrementRequestType.class)
            cnsiRequest.setGetAvailableIncrement((GetAvailableIncrementRequestType) requestData);
        else if (c == GetChecksumInfoRequestType.class)
            cnsiRequest.setGetChecksumInfo((ClassifierDetailsRequestByVersionType) requestData);
        else if (c == GetClassifierDataRequestType.class)
            cnsiRequest.setGetClassifierData((GetClassifierDataRequestType) requestData);
        else if (c == GetClassifierRecordsCountRequestType.class)
            cnsiRequest.setGetClassifierRecordsCount((GetClassifierRecordsCountRequestType) requestData);
        else if (c == GetClassifierRevisionListRequestType.class)
            cnsiRequest.setGetClassifierRevisionList((GetClassifierRevisionListRequestType) requestData);
        else if (c == GetClassifierRevisionsCountRequestType.class)
            cnsiRequest.setGetClassifierRevisionsCount((ClassifierDetailsRequestType) requestData);
        else if (c == GetClassifierStructureRequestType.class)
            cnsiRequest.setGetClassifierStructure((GetClassifierStructureRequestType) requestData);
        else if (c == ListClassifierGroupsRequestType.class)
            cnsiRequest.setListClassifierGroups((ListClassifierGroupsRequestType) requestData);
        else if (c == ListClassifiersRequestType.class)
            cnsiRequest.setListClassifiers((ListClassifiersRequestType) requestData);
        else
            throw new IllegalArgumentException("Invalid request type: " + requestData);
    }

    private <T> T getResponse(CnsiResponse response, Class<T> c) {
        if (c == GetAvailableIncrementResponseType.class)
            return c.cast(response.getGetAvailableIncrement());
        else if (c == GetChecksumInfoResponseType.class)
            return c.cast(response.getGetChecksumInfo());
        else if (c == GetClassifierDataResponseType.class)
            return c.cast(response.getGetClassifierData());
        else if (c == GetClassifierRecordsCountResponseType.class)
            return c.cast(response.getGetClassifierRecordsCount());
        else if (c == GetClassifierRevisionListResponseType.class)
            return c.cast(response.getGetClassifierRevisionList());
        else if (c == GetClassifierRevisionsCountResponseType.class)
            return c.cast(response.getGetClassifierRevisionsCount());
        else if (c == GetClassifierStructureResponseType.class)
            return c.cast(response.getGetClassifierStructure());
        else if (c == ListClassifierGroupsResponseType.class)
            return c.cast(response.getListClassifierGroups());
        else if (c == ListClassifiersResponseType.class)
            return c.cast(response.getListClassifiers());
        else
            throw new IllegalArgumentException("Invalid response type: " + c);
    }

    private <T> T extractResponse(ResponseDocument responseDocument, Class<T> c) {
        Element any = responseDocument.getSenderProvidedResponseData().getMessagePrimaryContent().getAny();
        try {
            Object unmarshal = JAXB_CTX.createUnmarshaller().unmarshal(any);
            return getResponse((CnsiResponse) unmarshal, c);
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmException(e);
        }
    }


}
