package ru.inovus.ms.rdm.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.RefBookCreateRequest;
import ru.inovus.ms.rdm.model.RefBookCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/refbook")
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
    RefBook getById(@PathParam("id") @ApiParam("Идентификатор версии") Integer versionId);

    @POST
    @ApiOperation("Создание нового справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook create(RefBookCreateRequest refBookCreateRequest);

}
