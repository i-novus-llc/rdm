package ru.inovus.ms.rdm.sync.model.loader;

import lombok.EqualsAndHashCode;
import ru.inovus.ms.rdm.api.exception.RdmException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@XmlRootElement(name = "mapping")
public class XmlMapping {

    public static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(XmlMapping.class);
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmException(e);
        }
    }

    private List<XmlMappingRefBook> refbooks;

    @XmlElement(name = "refbook")
    public List<XmlMappingRefBook> getRefbooks() {
        if(refbooks == null) {
            refbooks = new ArrayList<>();
        }
        return refbooks;
    }

    public void setRefbooks(List<XmlMappingRefBook> refbooks) {
        this.refbooks = refbooks;
    }
}
