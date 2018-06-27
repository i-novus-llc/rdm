package ru.inovus.ms.rdm.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/refBook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы со справочниками")
public interface RefBookService {

    @GET
    @ApiOperation("Поиск справочников по критериям")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список справочников"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBook> search(@BeanParam RefBookCriteria criteria);

    @GET
    @Path("/{id}")
    @ApiOperation("Поиск по идентификатору версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Passport getById(@PathParam("id") @ApiParam("Идентификатор версии") Integer versionId);

    @POST
    @Path("/create")
    @ApiOperation("Создание нового справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook create(RefBookCreateRequest refBookCreateRequest);

    @POST
    @Path("/update")
    @ApiOperation("Изменение метаданных справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook update(RefBookUpdateRequest refBookCreateRequest);

    @POST
    @Path("/archive/{refBookId}")
    @ApiOperation("В архив")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void archive(@PathParam("refBookId") @ApiParam("Идентификатор справочника") int refBookId);

    @GET
    @Path("/versions")
    @ApiOperation("Получение списка версий справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Версия справочника"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBookVersion> getVersions(@BeanParam VersionCriteria criteria);
}
