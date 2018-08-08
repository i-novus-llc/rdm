package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import ru.inovus.ms.rdm.model.FileModel;

import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/fileStorage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Api("Файловое хранилище")
public interface FileStorageService {

    @POST
    @ApiOperation("Сохранение файла")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "Файл",required = true,dataType = "java.io.File", paramType = "form"),
            @ApiImplicitParam(name = "fileName", value = "Имя файла",required = true,dataType = "String", paramType = "form")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "Файл сохранен"),
            @ApiResponse(code = 404, message = "Нет ресурса"),
            @ApiResponse(code = 400, message = "Неверный формат имени файла")
    })
    @Path("/save")
    FileModel save(@ApiParam(hidden = true)
                @Multipart(value = "file")
                InputStream is,
                @ApiParam(hidden = true)
                @Multipart(value = "fileName", required = true)
                @Pattern(regexp = "^[\\w@()-][\\w@()-.]*[\\w@()-]+$", message = "Invalid file name")
                String fileName);
}
