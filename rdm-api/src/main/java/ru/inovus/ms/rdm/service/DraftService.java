package ru.inovus.ms.rdm.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.model.*;

import javax.ws.rs.*;
import java.time.OffsetDateTime;

@Path("/draft")
@Produces("application/json")
@Consumes("application/json")
@Api("Методы работы с черновиками")
public interface DraftService {
    Draft create(Long dictionaryId, Metadata metadata);
    void updateMetadata(Long draftId, MetadataDiff metadataDiff);
    void updateData(Long draftId, DataDiff dataDiff);
    void updateData(Long draftId, FileData file);


    Data search(Long draftId, DraftCriteria criteria);

    @POST
    @Path("{draftId}/publish")
    @ApiOperation("Публикация черновика")
    void publish(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId, @ApiParam("Версия") @QueryParam("version") String version, @ApiParam("Дата публикации") @QueryParam("date") OffsetDateTime versionDate);

    void remove(Long draftId);

    Metadata getMetadata(Long draftId);


}
