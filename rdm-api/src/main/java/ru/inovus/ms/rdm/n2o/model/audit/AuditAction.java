package ru.inovus.ms.rdm.n2o.model.audit;

public enum AuditAction {
    PUBLICATION("Публикация"),
    UPLOAD("Загрузка справочника"),
    DOWNLOAD("Выгрузка справочника"),
    LOGIN("Вход в систему"),
    LOGOUT("Выход из системы"),
    CREATE_REF_BOOK("Создание справочника"),
    DELETE_REF_BOOK("Удаление справочника");

    private String name;

    AuditAction(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
