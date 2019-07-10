package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.model.ExportFile;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/compare")
@Api("Методы сравнения версий")
public interface DownloadCompareService {

    @GET
    @Path("/getFile")
    @ApiOperation("Результат сравнения в виде XLSX файла")
    @Produces("application/zip;charset=UTF-8")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    ExportFile getCompareFile(@ApiParam("Идентификатор старой версии") @QueryParam("oldVersionId") Integer oldVersionId,
                              @ApiParam("Идентификатор новой версии") @QueryParam("newVersionId") Integer newVersionId);
}