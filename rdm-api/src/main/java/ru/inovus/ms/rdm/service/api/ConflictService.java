package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.Conflict;
import ru.inovus.ms.rdm.model.RefBookVersion;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

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
    @ApiOperation("Проверка на наличие конфликта для двух версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Boolean checkConflicts(@ApiParam("Идентификатор версии, которая ссылается")
                           @QueryParam("refFromId")
                                   Integer refFromId,
                           @ApiParam("Идентификатор бесконфликтной версии, на которую ссылается")
                           @QueryParam("refToId")
                                   Integer refToId,
                           @ApiParam("Тип конфликта")
                           @QueryParam("type")
                                   ConflictType conflictType);

    @GET
    @Path("/{versionId}/check")
    @ApiOperation("Получение конфликтующих версий справочников")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<RefBookVersion> getConflictReferrers(@ApiParam("Идентификатор проверяемой версии")
                                              @PathParam("versionId")
                                                      Integer versionId,
                                              @ApiParam("Тип конфликта")
                                              @QueryParam("type")
                                                      ConflictType conflictType);

    @POST
    @Path("/create")
    @ApiOperation("Сохранение информации о конфликтах")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void create(@ApiParam("Идентификатор версии, которая ссылается")
                @QueryParam("refFromId")
                        Integer refFromId,
                @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются")
                @QueryParam("refToId")
                        Integer refToId,
                @ApiParam("Список конфликтов")
                        List<Conflict> conflicts);

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

    @POST
    @Path("/discover")
    @ApiOperation("Обнаружение конфликтов")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void discover(@ApiParam("Идентификатор старой версии, на которую ссылаются")
                  @QueryParam("oldVersionId")
                          Integer oldVersionId,
                  @ApiParam("Идентификатор новой версии, на которую будут ссылаться")
                  @QueryParam("newVersionId")
                          Integer newVersionId);
}
