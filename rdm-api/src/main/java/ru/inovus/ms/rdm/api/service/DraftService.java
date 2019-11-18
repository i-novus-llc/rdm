package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
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
    @ApiOperation("Создание нового черновика")
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
    @ApiOperation("Добавление или изменение записи черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/update/{draftId}")
    void updateData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                    @ApiParam("Запись черновика") Row row);

    @DELETE
    @ApiOperation("Удаление записи черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/delete/{draftId}/{systemId}")
    void deleteRow(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                   @ApiParam("Идентификатор записи") @PathParam("systemId") Long systemId);

    @DELETE
    @ApiOperation("Удаление всех записей черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/deleteAll/{draftId}")
    void deleteAllRows(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId);

    @POST
    @ApiOperation("Обновление черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })

    @Path("/updateFromFile/{draftId}")
    Draft updateData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                    @ApiParam("Файл") FileModel fileModel);

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

    @POST
    @Path("/attribute")
    @ApiOperation("Добавление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void createAttribute(@ApiParam("Модель создаваемого атрибута") CreateAttribute createAttribute);

    @PUT
    @Path("/attribute")
    @ApiOperation("Изменение атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttribute(@ApiParam("Модель изменяемого атрибута") UpdateAttribute updateAttribute);

    @DELETE
    @Path("/{versionId}/attribute/{code}")
    @ApiOperation("Удаление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void deleteAttribute(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                         @ApiParam("Код атрибута") @PathParam("code") String attributeCode);

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
