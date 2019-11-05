package ru.inovus.ms.rdm.esnsi;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Потребитель из очереди СМЭВ-3.
 */
@Component
class EsnsiSmevClient {

    private static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(EsnsiSmevClient.class);

    private static final String NAMESPACE_URI = "urn://x-artefacts-smev-gov-ru/esnsi/smev-integration/read/2.0.1";

    private static final String WSDL_URL = "./wsdl/adapter/v1_2/smev-service-adapter-1.2.wsdl";
    private static final QName SERVICE_QNAME = new QName("urn://x-artefacts-gov-ru/services/message-exchange/1.2", "SmevAdapterMessageExchangeService");

    @Value("${esnsi.smev-adapter.ws.url}")
    private String endpointURL;

    @Value("${esnsi.http.client.policy.timeout.receive}")
    private int receiveTimeout;

    @Value("${esnsi.http.client.policy.timeout.connection}")
    private int connectionTimeout;

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final Map<String, ResponseDocument> msgBuffer = new HashMap<>();

    private static final JAXBContext REQUEST_CTX;
    private static final JAXBContext RESPONSE_CTX;

    static {
        try {
            REQUEST_CTX = JAXBContext.newInstance(CnsiRequest.class);
            RESPONSE_CTX = JAXBContext.newInstance(CnsiRequest.class);
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmException(e);
        }
    }

    AcceptRequestDocument sendRequest(Object requestData, String messageId) {
        if (requestData.getClass() != CnsiRequest.class) {
            CnsiRequest cnsiRequest = objectFactory.createCnsiRequest();
            setRequest(cnsiRequest, requestData);
            requestData = cnsiRequest;
        }
        SendRequestDocument sendRequestDocument = objectFactory.createSendRequestDocument();
        sendRequestDocument.setMessageID(messageId);
        MessagePrimaryContent messagePrimaryContent = objectFactory.createMessagePrimaryContent();
        DOMResult domResult = new DOMResult();
        try {
            REQUEST_CTX.createMarshaller().marshal(requestData, domResult);
        } catch (JAXBException ex) {
            logger.error("Unable to create request from given request data: {}", requestData, ex);
            throw new RdmException(ex);
        }
        Document doc = (Document) domResult.getNode();
        messagePrimaryContent.setAny(doc.getDocumentElement());
        sendRequestDocument.setMessagePrimaryContent(messagePrimaryContent);
        try {
            return getSmevAdapterPort().sendRequest(sendRequestDocument);
        } catch (SmevAdapterFailureException ex) {
            logger.error("Error occurred while sending request message through SMEV3.", ex);
            return null;
        }
    }

    <T> Map.Entry<T, InputStream> getResponse(Class<T> tClass, String messageId) {
        return getResponse(tClass, messageId, 60, 1000);
    }

    <T> Map.Entry<T, InputStream> getResponse(Class<T> tClass, String messageId, int numRetries, int sleepMillis) {
        ResponseDocument responseDocument;
        int n = 0;
        do {
            responseDocument = getResponse(messageId);
            if (responseDocument != null) {
                InputStream inputStream = null;
                List<AttachmentContentType> attachmentContent = responseDocument.getAttachmentContentList().getAttachmentContent();
                if (!attachmentContent.isEmpty()) {
                    try {
                        inputStream = attachmentContent.iterator().next().getContent().getInputStream();
                    } catch (IOException e) {
                        logger.error("Cannot extract input stream from message attachment.", e);
                        throw new RdmException(e);
                    }
                }
                return Map.entry(extractResponse(responseDocument, tClass), inputStream == null ? EMPTY_INPUT_STREAM : inputStream);
            }
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                logger.error("Thread was unexpectedly interrupted.", e);
                throw new RdmException(e);
            }
        } while (++n < numRetries);
        return null;
    }

     private ResponseDocument getResponse(String messageId) {
        if (msgBuffer.containsKey(messageId))
            return msgBuffer.get(messageId);
        GetResponseDocument getResponseDocument = objectFactory.createGetResponseDocument();
        MessageTypeSelector messageTypeSelector = objectFactory.createMessageTypeSelector();
        messageTypeSelector.setNamespaceURI(NAMESPACE_URI);
        getResponseDocument.setMessageTypeSelector(messageTypeSelector);
        try {
            ResponseDocument response = getSmevAdapterPort().getResponse(getResponseDocument);
            if (response.getAttachmentContentList() == null && response.getMessageMetadata() == null &&
                response.getOriginalMessageId() == null && response.getOriginalTransactionCode() == null &&
                response.getReferenceMessageID() == null && response.getSenderProvidedResponseData() == null &&
                response.getSmevAdapterFault() == null && response.getSmevTypicalError() == null)
                return null;
            msgBuffer.put(response.getSenderProvidedResponseData().getMessageID(), response);
            if (response.getSenderProvidedResponseData().getMessageID().equals(messageId)) {
                return response;
            }
        } catch (SmevAdapterFailureException | UnknownMessageTypeException ex) {
            logger.error("Error occurred while receiving response message from SMEV3.", ex);
        }
        return null;
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

    private <T> T extractResponse(ResponseDocument responseDocument, Class<T> c) {
        Element any = responseDocument.getSenderProvidedResponseData().getMessagePrimaryContent().getAny();
        try {
            Object unmarshal = RESPONSE_CTX.createUnmarshaller().unmarshal(any);
            return c.cast(unmarshal);
        } catch (JAXBException e) {
            logger.error("Error while parsing response from SMEV3 adapter. Unknown format.", e);
            throw new RdmException(e);
        }
    }

    boolean acknowledge(String messageId) {
        msgBuffer.remove(messageId);
        AckRequest ackRequest = objectFactory.createAckRequest();
        ackRequest.setValue(messageId);
        try {
            getSmevAdapterPort().ack(ackRequest);
            return true;
        } catch (SmevAdapterFailureException | TargetMessageIsNotFoundException e) {
            logger.error("Error occurred while sending acknowledge message to SMEV3.", e);
            return false;
        }
    }

    private SmevAdapterMessageExchangePortType getSmevAdapterPort() {
        SmevAdapterMessageExchangePortType port = getServicePortType();
        initApacheCxfConfig(port);
        BindingProvider bp = (BindingProvider) port;
        setMTOMEnabled(port);
        setEndpointURL(bp, endpointURL);
        Client cxfClient = ClientProxy.getClient(port);
        setInterceptors(cxfClient);
        HTTPConduit httpConduit = (HTTPConduit) cxfClient.getConduit();
        HTTPClientPolicy policy = httpConduit.getClient();
        policy.setReceiveTimeout(receiveTimeout);
        policy.setConnectionTimeout(connectionTimeout);
        return port;
    }

    private SmevAdapterMessageExchangePortType getServicePortType() {
        URL wsdlUrl = Thread.currentThread().getContextClassLoader().getResource(WSDL_URL);
        SmevAdapterMessageExchangeService smevMessageExchangeService = new SmevAdapterMessageExchangeService(wsdlUrl, SERVICE_QNAME);
        return smevMessageExchangeService.getPort(SmevAdapterMessageExchangePortType.class);
    }

    private void initApacheCxfConfig(final Object port) {
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);
        Client client = JaxWsClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setClient(httpClientPolicy);
    }

    private void setMTOMEnabled(final Object port){
        Binding binding = ((BindingProvider) port).getBinding();
        ((SOAPBinding) binding).setMTOMEnabled(true);
    }

    private void setEndpointURL(final BindingProvider bp, final String endpointURL){
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
    }

    private void setInterceptors(Client cxfClient) {
        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setLimit(-1);
        cxfClient.getOutInterceptors().add(loggingOutInterceptor);
        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setLimit(-1);
        cxfClient.getInInterceptors().add(loggingInInterceptor);
    }

}
