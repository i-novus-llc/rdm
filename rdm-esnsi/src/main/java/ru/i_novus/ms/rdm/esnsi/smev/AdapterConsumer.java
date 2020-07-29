package ru.i_novus.ms.rdm.esnsi.smev;

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
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.esnsi.api.*;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;
import java.net.URL;

import static ru.i_novus.ms.rdm.esnsi.smev.Utils.JAXB_CTX;
import static ru.i_novus.ms.rdm.esnsi.smev.Utils.OBJECT_FACTORY;

/**
 * Потребитель из очереди СМЭВ-3.
 */
@Component
final class AdapterConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AdapterConsumer.class);

    private static final String NAMESPACE_URI = "urn://x-artefacts-smev-gov-ru/esnsi/smev-integration/read/2.0.1";

    private static final String WSDL_URL = "./wsdl/adapter/v1_2/smev-service-adapter-1.2.wsdl";
    private static final QName SERVICE_QNAME = new QName("urn://x-artefacts-gov-ru/services/message-exchange/1.2", "SmevAdapterMessageExchangeService");

    private SmevAdapterMessageExchangePortType port;

    AdapterConsumer(@Value("${esnsi.smev-adapter.ws.url}") String endpointURL,
                           @Value("${esnsi.http.client.policy.timeout.receive}") int receiveTimeout,
                           @Value("${esnsi.http.client.policy.timeout.connection}") int connectionTimeout) {
        port = getServicePortType();
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
    }

    AcceptRequestDocument sendRequest(Object requestData, String messageId) {
        SendRequestDocument sendRequestDocument = OBJECT_FACTORY.createSendRequestDocument();
        sendRequestDocument.setMessageID(messageId);
        MessagePrimaryContent messagePrimaryContent = OBJECT_FACTORY.createMessagePrimaryContent();
        DOMResult domResult = new DOMResult();
        try {
            JAXB_CTX.createMarshaller().marshal(requestData, domResult);
        } catch (JAXBException ex) {
            logger.error("Unable to create request from given request data: {}", requestData, ex);
            throw new RdmException(ex);
        }
        Document doc = (Document) domResult.getNode();
        messagePrimaryContent.setAny(doc.getDocumentElement());
        sendRequestDocument.setMessagePrimaryContent(messagePrimaryContent);
        try {
            return port.sendRequest(sendRequestDocument);
        } catch (Exception ex) {
            logger.error("Error occurred while sending request message through SMEV3.", ex);
            return null;
        }
    }

    ResponseDocument getResponseDocument() {
        GetResponseDocument getResponseDocument = OBJECT_FACTORY.createGetResponseDocument();
        MessageTypeSelector messageTypeSelector = OBJECT_FACTORY.createMessageTypeSelector();
        messageTypeSelector.setNamespaceURI(NAMESPACE_URI);
        getResponseDocument.setMessageTypeSelector(messageTypeSelector);
        try {
            ResponseDocument response = port.getResponse(getResponseDocument);
            if (nullResponse(response))
                return null;
            return response;
        } catch (Exception ex) {
            logger.error("Error occurred while receiving response message from SMEV3.", ex);
        }
        return null;
    }

    private boolean nullResponse(ResponseDocument resp) {
        boolean b1 = resp.getAttachmentContentList() == null && resp.getMessageMetadata() == null && resp.getOriginalMessageId() == null;
        boolean b2 = resp.getOriginalTransactionCode() == null && resp.getReferenceMessageID() == null && resp.getSenderProvidedResponseData() == null;
        boolean b3 = resp.getSmevAdapterFault() == null && resp.getSmevTypicalError() == null;
        return b1 && b2 && b3;
    }

    boolean acknowledge(String messageId) {
        AckRequest ackRequest = OBJECT_FACTORY.createAckRequest();
        ackRequest.setValue(messageId);
        try {
            port.ack(ackRequest);
            logger.info("Successfully acknowledged adapter about processing message with id {}", messageId);
            return true;
        } catch (Exception e) {
            logger.error("Error occurred while sending acknowledge message to SMEV3.", e);
            return false;
        }
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