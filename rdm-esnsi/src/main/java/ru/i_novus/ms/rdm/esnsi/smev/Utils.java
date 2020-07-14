package ru.i_novus.ms.rdm.esnsi.smev;

import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.esnsi.api.CnsiRequest;
import ru.i_novus.ms.rdm.esnsi.api.CnsiResponse;
import ru.i_novus.ms.rdm.esnsi.api.ObjectFactory;
import ru.i_novus.ms.rdm.esnsi.api.ResponseDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;

final class Utils {

    static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    static final JAXBContext JAXB_CTX;
    static {
        try {
            JAXB_CTX = JAXBContext.newInstance(ResponseDocument.class, CnsiResponse.class, CnsiRequest.class);
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmException(e);
        }
    }

    private Utils() {throw new UnsupportedOperationException();}

}
