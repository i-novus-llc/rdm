package ru.i_novus.ms.rdm.api.service;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;

import java.util.List;

/**
 * Черновик справочника: Сервис.
 */
public interface DraftService {

    /**
     * Создание черновика по запросу.
     *
     * @param request запрос
     * @return Созданный черновик
     */
    Draft create(CreateDraftRequest request);

    /**
     * Создание черновика на основе версии справочника.
     * <p/>
     * Если версия является черновиком, то возвращается этот черновик.
     *
     * @param versionId идентификатор версии
     * @return Созданный черновик с актуальными записями из версии или существующий черновик
     */
    Draft createFromVersion(Integer versionId);

    /**
     * Создание черновика из файла.
     *
     * @param refBookId идентификатор справочника
     * @param fileModel модель файла
     * @return Созданный черновик со структурой и данными из файла
     */
    Draft create(Integer refBookId, FileModel fileModel);

    /**
     * Добавление или изменение записей черновика.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void updateData(Integer draftId, UpdateDataRequest request);

    /**
     * Удаление записей черновика.
     * <p/>
     * Записи для удаления определяются либо по первичному ключу, либо по системному идентификатору.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void deleteData(Integer draftId, DeleteDataRequest request);

    /**
     * Удаление всех записей черновика.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void deleteAllData(Integer draftId, DeleteAllDataRequest request);

    /**
     * Обновление черновика из файла.
     * <p/>
     * В черновик добавляются данные из файла.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void updateFromFile(Integer draftId, UpdateFromFileRequest request);

    /**
     * Получение записей черновика по параметрам критерия.
     *
     * @param draftId  идентификатор черновика
     * @param criteria критерий поиска
     * @return Страница записей черновика
     */
    Page<RefBookRowValue> search(Integer draftId, SearchDataCriteria criteria);

    /**
     * Проверка на наличие записей в черновике.
     *
     * @param draftId идентификатор черновика
     * @return Результат проверки
     */
    Boolean hasData(Integer draftId);

    /**
     * Удаление черновика.
     *
     * @param draftId идентификатор черновика
     */
    void remove(Integer draftId);

    /**
     * Получение черновика по идентификатору.
     *
     * @param draftId идентификатор черновика
     * @return Черновик
     */
    Draft getDraft(Integer draftId);

    /**
     * Получение черновика по коду справочника.
     *
     * @param refBookCode код справочника
     * @return Черновик
     */
    Draft findDraft(String refBookCode);

    /**
     * Добавление атрибута справочника.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void createAttribute(Integer draftId, CreateAttributeRequest request);

    /**
     * Изменение атрибута справочника.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void updateAttribute(Integer draftId, UpdateAttributeRequest request);

    /**
     * Удаление атрибута справочника.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void deleteAttribute(Integer draftId, DeleteAttributeRequest request);

    /**
     * Добавление настраиваемой проверки атрибута.
     *
     * @param draftId             идентификатор черновика
     * @param attribute           код атрибута
     * @param attributeValidation проверка атрибута
     */
    void addAttributeValidation(Integer draftId, String attribute, AttributeValidation attributeValidation);

    /**
     * Удаление настраиваемой проверки атрибута.
     *
     * @param draftId   идентификатор черновика
     * @param attribute код атрибута
     * @param type      тип проверки атрибута
     */
    void deleteAttributeValidation(Integer draftId, String attribute, AttributeValidationType type);

    /**
     * Получение настраиваемых проверок атрибута (или атрибутов).
     *
     * @param draftId   идентификатор черновика
     * @param attribute код атрибута, может быть null
     * @return Список настраиваемых проверок
     */
    List<AttributeValidation> getAttributeValidations(Integer draftId, String attribute);

    /**
     * Обновление настраиваемых проверок атрибута.
     *
     * @param draftId идентификатор черновика
     * @param request запрос
     */
    void updateAttributeValidations(Integer draftId, AttributeValidationRequest request);

    /**
     * Выгрузка черновика в файл.
     *
     * @param draftId  идентификатор черновика
     * @param fileType тип файла
     * @return Файл черновика справочника
     */
    ExportFile getDraftFile(Integer draftId, FileType fileType);
}
