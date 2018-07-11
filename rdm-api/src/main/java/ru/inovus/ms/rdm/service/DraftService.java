package ru.inovus.ms.rdm.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.*;

import javax.ws.rs.*;
import java.time.LocalDateTime;

@Path("/draft")
@Produces("application/json")
@Consumes("application/json")
@Api("Методы работы с черновиками")
public interface DraftService {
    @POST
    @ApiOperation("Создание нового черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик создан"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Draft create(Integer refBookId, Structure structure);

    void updateMetadata(Integer draftId, MetadataDiff metadataDiff);

    void updateData(Integer draftId, DataDiff dataDiff);

    void updateData(Integer draftId, FileData file);

    @GET
    @ApiOperation("Получения записей черновика, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет черновика")
    })
    Page<RowValue> search(Integer draftId, SearchDataCriteria criteria);

    @POST
    @Path("{draftId}/publish")
    @ApiOperation("Публикация черновика")
    void publish(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                 @ApiParam("Версия") @QueryParam("version") String version,
                 @ApiParam("Дата начала действия версии") @QueryParam("fromDate") LocalDateTime fromDate,
                 @ApiParam("Дата окончания действия версии") @QueryParam("toDate") LocalDateTime toDate);

    void remove(Integer draftId);

    Structure getMetadata(Integer draftId);

    Draft getDraft(Integer draftId);

}
