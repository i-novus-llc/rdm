package ru.inovus.ms.rdm.sync.model;


public enum DataTypeEnum {
    VARCHAR("varchar", "STRING"),
    INTEGER("bigint", "INTEGER"),
    DATE("date", "DATE"),
    BOOLEAN("boolean", "BOOLEAN"),
    FLOAT("numeric", "FLOAT"),
    JSONB("jsonb", "REFERENCE");

    private String text;
    private String nsiDataType;

    DataTypeEnum(String text, String nsiDataType) {
        this.text = text;
        this.nsiDataType = nsiDataType;
    }

    public String getText() {
        return text;
    }

    public String getNsiDataType() {
        return nsiDataType;
    }

    public static DataTypeEnum getByText(String text) {
        if (text == null)
            return null;
        for (DataTypeEnum value : values()) {
            if (value.getText().equals(text)) {
                return value;
            }
        }
        return null;
    }

    public static DataTypeEnum getByNsiDataType(String nsiDataType) {
        if (nsiDataType == null)
            return null;
        for (DataTypeEnum value : values()) {
            if (value.getNsiDataType().equals(nsiDataType)) {
                return value;
            }
        }
        return null;
    }
}

