package ru.inovus.ms.rdm.sync.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.refdata.ChangeDataRequest;
import ru.inovus.ms.rdm.sync.criteria.LogCriteria;
import ru.inovus.ms.rdm.sync.model.Log;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@Path("rdm")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Синхронизация данных справочников НСИ", hidden = true)
public interface RdmSyncRest {

    @POST
    @Path("/update")
    @ApiOperation(value = "Синхронизация всех справочников")
    void update();

    @POST
    @Path("/update/{refbookCode}")
    @ApiOperation(value = "Синхронизация отдельного справочника")
    void update(@PathParam("refbookCode") String refbookCode);

    void update(RefBook refBook, VersionMapping versionMapping);

    @GET
    @Path("/log")
    @ApiOperation(value = "Получение журнала за дату")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Успех"),
        @ApiResponse(code = 400, message = "Укажите пожалуйста дату в формате ISO_LOCAL_DATE [yyyy-MM-dd].")
    })
    List<Log> getLog(@BeanParam LogCriteria criteria);

    @POST
    @Path("/pullInRdm")
    @ApiOperation(value = "Пульнуть данные в рдм")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Успех")
    })
    void pullInRdm(ChangeDataRequest request);

    @POST
    @Path("/pullInRdm")
    @ApiOperation(value = "Пульнуть данные в рдм")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Успех")
    })
    void pullInRdm(String refBookCode, List<Object> addUpdate, List<Object> delete);

    @POST
    @Path("/pullInRdmAsync")
    @ApiOperation(value = "Пульнуть данные в рдм (через очередь сообщений)")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Сообщение поставлено в очередь"),
            @ApiResponse(code = 500, message = "Брокер сообщений не доступен"),
            @ApiResponse(code = 501, message = "Очередь сообщений не сконфигурирована")
    })
    void pullInRdmAsync(String refBookCode, List<Object> addUpdate, List<Object> delete);

}
