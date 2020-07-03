package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refdata.*;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.inovus.ms.rdm.api.model.version.CreateAttribute;
import ru.inovus.ms.rdm.api.model.version.UpdateAttribute;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/draft")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы работы с черновиками", hidden = true)
public interface DraftService {

    @POST
    @ApiOperation("Создание нового черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Draft create(CreateDraftRequest createDraftRequest);

    @POST
    @ApiOperation("Создание нового черновика на основе версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/createFromVersion/{versionId}")
    Draft createFromVersion(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId);

    @POST
    @ApiOperation("Создание нового черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/createByFile/{refBookId}")
    Draft create(@ApiParam("Идентификатор справочника") @PathParam("refBookId") Integer refBookId,
                 @ApiParam("Файл") FileModel fileModel);

    @POST
    @ApiOperation("Добавление или изменение записей черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/updateData")
    void updateData(UpdateDataRequest request);

    @DELETE
    @ApiOperation(value = "Удаление записей черновика (либо по первичному ключу, либо по системному идентификатору)")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/deleteData")
    void deleteData(DeleteDataRequest request);

    @DELETE
    @ApiOperation("Удаление всех записей черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/deleteAllData")
    void deleteAllData(DeleteAllDataRequest request);

    @POST
    @ApiOperation("Обновление черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })

    @Path("/updateFromFile")
    void updateFromFile(UpdateFromFileRequest request);

    @GET
    @Path("/{draftId}/data")
    @ApiOperation("Получение записей черновика по параметрам критерия")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBookRowValue> search(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                                 @BeanParam SearchDataCriteria criteria);

    @GET
    @Path("/{draftId}/hasData")
    @ApiOperation("Проверка на наличие записей в черновике")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Boolean hasData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId);

    @POST
    @Path("{draftId}/remove")
    @ApiOperation("Удаление черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик удален"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void remove(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId);

    @GET
    @Path("/{draftId}")
    @ApiOperation("Получение черновика по идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик найден"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Draft getDraft(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId);

    @GET
    @Path("/refBook/{refBookCode}")
    @ApiOperation(value = "Получение черновика по коду справочника", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Draft findDraft(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode);

    @POST
    @Path("/attribute")
    @ApiOperation("Добавление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void createAttribute(@ApiParam("Модель создаваемого атрибута") CreateAttribute createAttribute,
                         @ApiParam("Значение оптимистической блокировки версии") @QueryParam("optLockValue") Integer optLockValue);

    @PUT
    @Path("/attribute")
    @ApiOperation("Изменение атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttribute(@ApiParam("Модель изменяемого атрибута") UpdateAttribute updateAttribute,
                         @ApiParam("Значение оптимистической блокировки версии") @QueryParam("optLockValue") Integer optLockValue);

    @DELETE
    @Path("/{versionId}/attribute/{code}")
    @ApiOperation("Удаление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void deleteAttribute(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                         @ApiParam("Код атрибута") @PathParam("code") String attributeCode,
                         @ApiParam("Значение оптимистической блокировки версии") @QueryParam("optLockValue") Integer optLockValue);

    @POST
    @Path("/{versionId}/attribute/{attribute}/validation")
    @ApiOperation("Добавление настраиваемой проверки")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void addAttributeValidation(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                @ApiParam("Атрибут") @PathParam("attribute") String attribute,
                                @ApiParam("Пользовательская проверка") AttributeValidation attributeValidation);

    @DELETE
    @Path("/{versionId}/attributeValidation")
    @ApiOperation("Удаление настраиваемой проверки")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void deleteAttributeValidation(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                   @ApiParam("Атрибут") @QueryParam("attribute") String attribute,
                                   @ApiParam("Тип проверки") @QueryParam("type") AttributeValidationType type);


    @GET
    @Path("/{versionId}/attributeValidations")
    @ApiOperation("Получение настраиваемых проверок")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<AttributeValidation> getAttributeValidations(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                                      @ApiParam("Атрибут") @QueryParam("attribute") String attribute);

    @PUT
    @Path("/{versionId}/attribute")
    @ApiOperation("Обновление настраиваемых проверок")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttributeValidations(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                    @ApiParam("Запрос") AttributeValidationRequest request);

    @GET
    @Path("/{draftId}/getFile")
    @Produces("application/zip")
    @ApiOperation("Выгрузка черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    ExportFile getDraftFile(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                            @ApiParam(value = "Тип файла", required = true, allowableValues = "XLSX, XML") @QueryParam("type") FileType fileType);
}
