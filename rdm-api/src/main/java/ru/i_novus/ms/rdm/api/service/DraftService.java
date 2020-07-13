package ru.i_novus.ms.rdm.api.service;

import io.swagger.annotations.*;
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
    @Path("/fromVersion/{versionId}")
    Draft createFromVersion(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId);

    @POST
    @ApiOperation("Создание нового черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/fromFile/{refBookId}")
    Draft create(@ApiParam("Идентификатор справочника") @PathParam("refBookId") Integer refBookId,
                 @ApiParam("Файл") FileModel fileModel);

    @POST
    @ApiOperation("Добавление или изменение записей черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{draftId}/data")
    void updateData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                    UpdateDataRequest request);

    @DELETE
    @ApiOperation(value = "Удаление записей черновика (либо по первичному ключу, либо по системному идентификатору)")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{draftId}/data")
    void deleteData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                    DeleteDataRequest request);

    @DELETE
    @ApiOperation("Удаление всех записей черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{draftId}/allData")
    void deleteAllData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                       DeleteAllDataRequest request);

    @POST
    @ApiOperation("Обновление черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{draftId}/fromFile")
    void updateFromFile(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                        UpdateFromFileRequest request);

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
    @Path("/{draftId}/attribute")
    @ApiOperation("Добавление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void createAttribute(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                         @ApiParam("Модель создаваемого атрибута") CreateAttributeRequest request);

    @PUT
    @Path("/{draftId}/attribute")
    @ApiOperation("Изменение атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttribute(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                         @ApiParam("Модель изменяемого атрибута") UpdateAttributeRequest request);

    @DELETE
    @Path("/{draftId}/attribute")
    @ApiOperation("Удаление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void deleteAttribute(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                         @ApiParam("Модель удаляемого атрибута") DeleteAttributeRequest request);

    @POST
    @Path("/{draftId}/attributeValidation/{attribute}")
    @ApiOperation("Добавление настраиваемой проверки")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void addAttributeValidation(@ApiParam("Идентификатор версии") @PathParam("draftId") Integer draftId,
                                @ApiParam("Атрибут") @PathParam("attribute") String attribute,
                                @ApiParam("Пользовательская проверка") AttributeValidation attributeValidation);

    @DELETE
    @Path("/{draftId}/attributeValidation")
    @ApiOperation("Удаление настраиваемой проверки")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void deleteAttributeValidation(@ApiParam("Идентификатор версии") @PathParam("draftId") Integer draftId,
                                   @ApiParam("Атрибут") @QueryParam("attribute") String attribute,
                                   @ApiParam("Тип проверки") @QueryParam("type") AttributeValidationType type);


    @GET
    @Path("/{draftId}/attributeValidations")
    @ApiOperation("Получение настраиваемых проверок")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<AttributeValidation> getAttributeValidations(@ApiParam("Идентификатор версии") @PathParam("draftId") Integer draftId,
                                                      @ApiParam("Атрибут") @QueryParam("attribute") String attribute);

    @PUT
    @Path("/{draftId}/attributeValidations")
    @ApiOperation("Обновление настраиваемых проверок")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttributeValidations(@ApiParam("Идентификатор версии") @PathParam("draftId") Integer draftId,
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
