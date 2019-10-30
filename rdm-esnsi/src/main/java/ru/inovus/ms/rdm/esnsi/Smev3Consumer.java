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
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import ru.inovus.ms.rdm.esnsi.api.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;
import java.net.URL;
import java.util.UUID;

@Service
/**
 * Потребитель из очереди СМЭВ-3.
 */
public class Smev3Consumer {

    private static Logger logger = LoggerFactory.getLogger(Smev3Consumer.class);

    private static final String WSDL_URL = "wsdl/adapter/v1_2/smev-service-adapter-1.2.wsdl";
    private static final QName SERVICE_QNAME = new QName("urn://x-artefacts-gov-ru/services/message-exchange/1.2", "SmevAdapterMessageExchangeService");

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @Value("${esnsi.smev-adapter.ws.url}")
    private String endpointURL;

    @Value("${esnsi.http.client.policy.timeout.receive}")
    private int receiveTimeout;

    @Value("${esnsi.http.client.policy.timeout.connection}")
    private int connectionTimeout;

    public AcceptRequestDocument sendRequest(Object requestData, UUID messageId) throws SmevAdapterFailureException, JAXBException {
        SendRequestDocument sendRequestDocument = new SendRequestDocument();
        sendRequestDocument.setMessageID(messageId.toString());
        MessagePrimaryContent messagePrimaryContent = new MessagePrimaryContent();
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(requestData.getClass());
        } catch (JAXBException ex) {
            logger.error("Error while instantiating JAXBContext for request {}. Check that this is a valid WSDL class.", requestData, ex);
            throw ex;
        }
        DOMResult domResult = new DOMResult();
        jaxbContext.createMarshaller().marshal(requestData, domResult);
        messagePrimaryContent.setAny((Element) domResult.getNode());
        try {
            return getSmevAdapterPort().sendRequest(sendRequestDocument);
        } catch (SmevAdapterFailureException ex) {
            logger.error("Error occurred while sending request message through SMEV3.", ex);
            throw ex;
        }
    }

    public ResponseDocument getResponse(Class<?> requestType) throws SmevAdapterFailureException, UnknownMessageTypeException {
        GetResponseDocument getResponseDocument = new GetResponseDocument();
        MessageTypeSelector messageTypeSelector = new MessageTypeSelector();
        XmlRootElement xmlRootElement = requestType.getAnnotation(XmlRootElement.class);
        if (xmlRootElement == null)
            throw new IllegalArgumentException("Class " + requestType + " is non valid WSDL type.");
        messageTypeSelector.setNamespaceURI(xmlRootElement.namespace());
        messageTypeSelector.setChildRootElementLocalName(xmlRootElement.name());
        getResponseDocument.setMessageTypeSelector(messageTypeSelector);
        try {
            return getSmevAdapterPort().getResponse(getResponseDocument);
        } catch (SmevAdapterFailureException | UnknownMessageTypeException ex) {
            logger.error("Error occurred while receiving response message from SMEV3.", ex);
            throw ex;
        }
    }

    public boolean acknowledge(UUID messageId) {
        AckRequest ackRequest = new AckRequest();
        ackRequest.setValue(messageId.toString());
        try {
            getSmevAdapterPort().ack(ackRequest);
            return true;
        } catch (SmevAdapterFailureException | TargetMessageIsNotFoundException e) {
            logger.error("Error occurred while sending acknowledge message to SMEV3.");
            return false;
        }
    }

    private SmevAdapterMessageExchangePortType getSmevAdapterPort() {
        SmevAdapterMessageExchangePortType port = getServicePortType();
        initApacheCxfConfig(port);
        BindingProvider bp = (BindingProvider) port;
        setMTOMEnabled(port, true);
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

    private void setMTOMEnabled(final Object port, final boolean isMTOMEnabled){
        Binding binding = ((BindingProvider) port).getBinding();
        ((SOAPBinding) binding).setMTOMEnabled(isMTOMEnabled);
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
