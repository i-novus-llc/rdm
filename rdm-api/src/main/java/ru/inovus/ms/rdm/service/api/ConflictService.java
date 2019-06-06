package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.Conflict;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/conflicts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы с конфликтами")
public interface ConflictService {

    @GET
    @Path("/calculate")
    @ApiOperation("Вычисление конфликтов для двух версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<Conflict> calculateConflicts(@ApiParam("Идентификатор версии, которая ссылается")
                                      @QueryParam("refFromId")
                                              Integer refFromId,
                                      @ApiParam("Идентификатор бесконфликтной версии, на которую ссылается")
                                      @QueryParam("refToId")
                                              Integer refToId);

    @GET
    @Path("/check")
    @ApiOperation("Проверка на наличие конфликтов для двух версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Map<ConflictType, Boolean> checkConflicts(@ApiParam("Идентификатор версии, которая ссылается")
                                              @QueryParam("refFromId")
                                                      Integer refFromId,
                                              @ApiParam("Идентификатор бесконфликтной версии, на которую ссылается")
                                              @QueryParam("refToId")
                                                      Integer refToId);

    @POST
    @Path("/update/displayvalue")
    @ApiOperation("Обновление отображаемых значений ссылок")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void updateReferenceValues(@ApiParam("Идентификатор версии, которая ссылается")
                               @QueryParam("refFromId")
                                       Integer refFromId,
                               @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются")
                               @QueryParam("refToId")
                                       Integer refToId,
                               @ApiParam("Список конфликтов")
                                       List<Conflict> conflicts);
}
