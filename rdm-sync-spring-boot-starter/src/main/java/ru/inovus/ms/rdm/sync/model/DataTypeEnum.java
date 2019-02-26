package ru.inovus.ms.rdm.sync.model;


public enum DataTypeEnum {
    VARCHAR("varchar"),
    INTEGER("bigint"),
    DATE("date"),
    BOOLEAN("boolean"),
    FLOAT("numeric"),
    JSONB("jsonb");

    private String text;

    DataTypeEnum(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
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
}

