package ru.inovus.ms.rdm.sync.model.loader;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@XmlRootElement(name = "mapping")
public class XmlMapping {

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
