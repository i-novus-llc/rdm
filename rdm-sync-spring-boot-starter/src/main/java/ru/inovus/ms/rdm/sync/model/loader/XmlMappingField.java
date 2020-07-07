package ru.inovus.ms.rdm.sync.model.loader;

import lombok.EqualsAndHashCode;
import ru.inovus.ms.rdm.sync.model.FieldMapping;

import javax.xml.bind.annotation.XmlAttribute;

@EqualsAndHashCode
public class XmlMappingField {

    private String rdmField;

    private String sysDataType;

    private String sysField;

    @XmlAttribute(name = "rdm-field", required = true)
    public String getRdmField() {
        return rdmField;
    }

    public void setRdmField(String rdmField) {
        this.rdmField = rdmField;
    }

    @XmlAttribute(name = "sys-data-type", required = true)
    public String getSysDataType() {
        return sysDataType;
    }

    public void setSysDataType(String sysDataType) {
        this.sysDataType = sysDataType;
    }

    @XmlAttribute(name = "sys-field", required = true)
    public String getSysField() {
        return sysField;
    }

    public void setSysField(String sysField) {
        this.sysField = sysField;
    }

    public static XmlMappingField createBy(FieldMapping fm) {
        XmlMappingField xmlMappingField = new XmlMappingField();
        xmlMappingField.setSysDataType(fm.getSysDataType());
        xmlMappingField.setSysField(fm.getSysField());
        xmlMappingField.setRdmField(fm.getRdmField());
        return xmlMappingField;
    }

}
