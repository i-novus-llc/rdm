package ru.inovus.ms.rdm.service.api;

import com.sun.rowset.internal.Row;
import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.*;

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
    @ApiOperation("Создание нового черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Draft create(Integer refBookId, MultipartFile file);

    void updateMetadata(Integer draftId, MetadataDiff metadataDiff);

    void updateData(Integer draftId, DataDiff dataDiff);

    void addData(List<Row> rows);

    void updateData(Long rowId, Row newRow);

    void deleteData(Long rowId);

    @POST
    @ApiOperation("Обновления черновика из файла")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик обновлен"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/update/{draftId}")
    void updateData(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId, FileModel fileModel);

    @GET
    @Path("/{draftId}/data")
    @ApiOperation("Получения записей черновика, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет черновика")
    })
    Page<RowValue> search(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId, @BeanParam SearchDataCriteria criteria);

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
    void createAttribute(
            @ApiParam("Идентификатор версии") @QueryParam("versionId") Integer versionId,
            @ApiParam("Модель данных атрибута") Structure.Attribute attribute,
            @ApiParam("Версия ссылки") @QueryParam("referenceVersion") Integer referenceVersion,
            @ApiParam("Атрибут ссылки") @QueryParam("referenceAttribute") String referenceAttribute,
            @ApiParam("Отображаемый атрибут") @QueryParam("referenceDisplayAttribute")
            List<String> referenceDisplayAttributes);

    @PUT
    @Path("/attribute")
    @ApiOperation("Изменение атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateAttribute(
            @ApiParam("Идентификатор версии") @QueryParam("versionId") Integer versionId,
            @ApiParam("Модель данных атрибута") Structure.Attribute attribute,
            @ApiParam("Версия ссылки") @QueryParam("referenceVersion") Integer referenceVersion,
            @ApiParam("Атрибут ссылки") @QueryParam("referenceAttribute") String referenceAttribute,
            @ApiParam("Отображаемый атрибут") @QueryParam("referenceDisplayAttribute")
            List<String> referenceDisplayAttributes);

    @DELETE
    @Path("/attribute")
    @ApiOperation("Удаление атрибута справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void deleteAttribute(@ApiParam("Идентификатор версии") @QueryParam("versionId") Integer versionId,
                         @ApiParam("Код атрибута") @QueryParam("code") String attributeCode);
}
