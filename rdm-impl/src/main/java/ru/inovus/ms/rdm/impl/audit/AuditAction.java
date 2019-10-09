package ru.inovus.ms.rdm.impl.audit;

import java.util.Map;
import java.util.function.Function;

import static ru.inovus.ms.rdm.impl.audit.AuditConstants.*;

public enum AuditAction {

    PUBLICATION(
        "Публикация версии справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY,
            REFBOOK_VERSION_KEY
        )
    ),

    UPLOAD_VERSION_FROM_FILE( // Создаем черновик и тут же в него заливаем данные
        "Загрузка черновика справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY,
            REFBOOK_STRUCTURE_KEY
        )
    ),

    UPLOAD_DATA( // Заливаем в уже существующий черновик
        "Загрузка данных черновика справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY
        )
    ),

    DRAFT_EDITING(
        "Редактирование черновика справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY
        )
    ),

    DOWNLOAD(
        "Выгрузка справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY,
            REFBOOK_VERSION_KEY
        )
    ),

    CREATE_REF_BOOK(
        "Создание справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY,
            REFBOOK_STRUCTURE_KEY
        )
    ),

    DELETE_REF_BOOK(
        "Удаление справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY
        )
    ),

    ARCHIVE(
        "Перевод справочника в архив",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY
        )
    ),

    EDIT_STRUCTURE(
        "Редактирование структуры черновика справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY
        )
    ),

    EDIT_PASSPORT(
        "Редактирование паспорта справочника",
        OBJ_NAME_REFBOOK,
        OBJ_TYPE_REFBOOK,
        GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY,
        refBookCtxExtract(
            REFBOOK_CODE_KEY,
            REFBOOK_NAME_KEY,
            REFBOOK_SHORT_NAME_KEY
        )
    );

    private String name;
    private String objName;
    private String objType;
    private Function<Object, String> getObjId;
    private Function<Object, Map<String, Object>> getContext;

    AuditAction(String name, String objName, String objType, Function<Object, String> getObjId, Function<Object, Map<String, Object>> getContext) {
        this.name = name;
        this.objName = objName;
        this.objType = objType;
        this.getObjId = getObjId;
        this.getContext = getContext;
    }

    public String getName() {return name;}
    public String getObjName() {return objName;}
    public String getObjType() {return objType;}

    public String getObjId(Object obj) {
        return getObjId.apply(obj);
    }

    public Map<String, Object> getContext(Object obj) {
        return getContext.apply(obj);
    }

}
