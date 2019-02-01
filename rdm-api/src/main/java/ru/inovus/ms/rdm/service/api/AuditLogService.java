package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.audit.AuditLog;
import ru.inovus.ms.rdm.model.audit.AuditLogCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Аудит действий пользователя")
public interface AuditLogService {

    @POST
    @ApiOperation("Добавить действие")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Файл сохранен"),
            @ApiResponse(code = 404, message = "Нет ресурса"),
    })
    @Path("/action")
    AuditLog addAction(@ApiParam("Действие пользователя") AuditLog action);

    @GET
    @ApiOperation("Список действий по фильтрам")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Файл сохранен"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса"),
    })
    @Path("/actions")
    Page<AuditLog> getActions(@ApiParam("Критерий поиска действий") @BeanParam AuditLogCriteria criteria);
}
