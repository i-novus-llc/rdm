package ru.inovus.ms.rdm.api.model.audit;

import java.util.Map;
import java.util.function.Function;

public enum AuditAction {

    PUBLICATION(
        "Публикация",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    ),

    UPLOAD_VERSION_FROM_FILE( // Создаем черновик и тут же в него заливаем данные
        "Загрузка версии справочника",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    ),

    UPLOAD_DATA( // Заливаем в уже существующий черновик
        "Загрузка данных справочника",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    ),

    DRAFT_EDITING(
        "Редактирование черновика справочника",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    ),

    DOWNLOAD(
        "Выгрузка справочника",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    ),

    CREATE_REF_BOOK(
        "Создание справочника",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    ),

    DELETE_REF_BOOK(
        "Удаление справочника",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    ),

    ARCHIVE(
        "Перевод справочника в архив",
        "Справочник",
        "refBook",
        ctx -> ctx.get("refBookId")
    );

    private String name;
    private String objName;
    private String objType;
    private Function<Map<String, String>, String> getObjId;

    AuditAction(String name,
                String objName,
                String objType,
                Function<Map<String, String>, String> getObjId) {
        this.name = name;
        this.objName = objName;
        this.objType = objType;
        this.getObjId = getObjId;
    }

    public String getName() {return name;}
    public String getObjName() {return objName;}
    public String getObjType() {return objType;}

    public String getObjId(Map<String, String> ctx) {
        return getObjId.apply(ctx);
    }

}
