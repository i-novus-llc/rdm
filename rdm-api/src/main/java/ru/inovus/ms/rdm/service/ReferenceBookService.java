package ru.inovus.ms.rdm.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.ReferenceBook;
import ru.inovus.ms.rdm.model.ReferenceBookCreateRequest;
import ru.inovus.ms.rdm.model.ReferenceBookCriteria;

import javax.ws.rs.*;
import javax.xml.ws.Response;

@Path("/refbook")
@Produces("application/json")
@Consumes("application/json")
@Api("Методы работы со справочниками")
public interface ReferenceBookService {

    @GET
    @ApiOperation("Получения списка справочников, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<ReferenceBook> search(@BeanParam ReferenceBookCriteria referenceBookCriteria);

    @POST
    @ApiOperation("Создание нового справочника")
    ReferenceBook create(@BeanParam ReferenceBookCreateRequest referenceBookCreateRequest);

}
