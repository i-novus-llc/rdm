package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.model.validation.AttributeValidationType;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;

@Path("/draft")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы с черновиками")
public interface DraftService {

    @POST
    @ApiOperation("Создание нового черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/create/{refBookId}")
    Draft create(@ApiParam("Идентификатор справочника") @PathParam("refBookId") Integer refBookId, Structure structure);

    @POST
    @ApiOperation("Создание нового черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/createFromVersion/{versionId}")
    Draft createFromVersion(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId);

    @POST
    @ApiOperation("Создание нового черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/createByFile/{refBookId}")
    Draft create(@ApiParam("Идентификатор справочника") @PathParam("refBookId") Integer refBookId, FileModel fileModel);

    void updateMetadata(Integer draftId, MetadataDiff metadataDiff);

    void updateData(Integer draftId, DataDiff dataDiff);

    @POST
    @ApiOperation("Добавление или изменение строки черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/update/{draftId}")
    void updateData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId, Row row);

    @DELETE
    @ApiOperation("Удаление строки черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/delete/{draftId}/{systemId}")
    void deleteRow(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                   @ApiParam("Идентификатор строки") @PathParam("systemId") Long systemId);

    @DELETE
    @ApiOperation("Удаление строк черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/deleteAll/{draftId}")
    void deleteAllRows(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId);

    @POST
    @ApiOperation("Обновление черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })

    @Path("/updateFromFile/{draftId}")
    void updateData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId, FileModel fileModel);

    @GET
    @Path("/{draftId}/data")
    @ApiOperation("Получение записей черновика, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет черновика")
    })
    Page<RefBookRowValue> search(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                          @BeanParam SearchDataCriteria criteria);

    @POST
    @Path("{draftId}/publish")
    @ApiOperation("Публикация черновика")
    void publish(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                 @ApiParam("Версия") @QueryParam("version") String version,
                 @ApiParam("Дата начала действия версии") @QueryParam("fromDate") LocalDateTime fromDate,
                 @ApiParam("Дата окончания действия версии") @QueryParam("toDate") LocalDateTime toDate);

    @POST
    @Path("{draftId}/remove")
    @ApiOperation("Удаление черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик удален"),
            @ApiResponse(code = 404, message = "Нет черновика")
    })
    void remove(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId);

    Structure getMetadata(Integer draftId);

    @GET
    @Path("/{draftId}")
    @ApiOperation("Получение черновика по идентификатору")
    Draft getDraft(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId);

    @POST
    @Path("/attribute")
    @ApiOperation("Добавление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void createAttribute(@ApiParam("Модель атрибута справочника") CreateAttribute createAttribute);

    @PUT
    @Path("/attribute")
    @ApiOperation("Изменение атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttribute(@ApiParam("Модель атрибута справочника") UpdateAttribute updateAttribute);

    @DELETE
    @Path("/attribute")
    @ApiOperation("Удаление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void deleteAttribute(@ApiParam("Идентификатор версии") @QueryParam("versionId") Integer versionId,
                         @ApiParam("Код атрибута") @QueryParam("code") String attributeCode);

    @POST
    @Path("/{versionId}/attribute/{attribute}/validation")
    @ApiOperation("Добавление настраиваемой проверки")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
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
    List<AttributeValidation> getAttributeValidations(@ApiParam("Идентификатор версии") @PathParam("versionId")
                                                                   Integer versionId,
                                                      @ApiParam("Атрибут") @QueryParam("attribute")
                                                                   String attribute);


    @PUT
    @Path("/{versionId}/attribute/{attribute}")
    @ApiOperation("Обновление настраиваемых проверок")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttributeValidations(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                    @ApiParam("Атрибут") @PathParam("attribute") String attribute,
                                    @ApiParam("Настраиваемые проверки") List<AttributeValidation> validations);

    @GET
    @Path("/{draftId}/getFile")
    @Produces("application/zip")
    @ApiOperation("Выгрузка черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    ExportFile getDraftFile(@ApiParam("Идентификатор версии")
                          @PathParam("draftId")
                          Integer draftId,
                          @ApiParam(value = "Тип файла", required = true)
                          @QueryParam("type")
                          FileType fileType);
}
