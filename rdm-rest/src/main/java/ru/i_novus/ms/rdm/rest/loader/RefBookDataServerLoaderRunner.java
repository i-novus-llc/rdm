package ru.i_novus.ms.rdm.rest.loader;

import io.swagger.annotations.*;
import net.n2oapp.platform.loader.server.*;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.service.FileStorageService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Запускатель загрузчиков справочников RDM.
 */
@Service
@Api("Загрузчики справочников")
@SuppressWarnings({"rawtypes", "java:S3740"})
public class RefBookDataServerLoaderRunner extends BaseLoaderRunner implements ServerLoaderRestService {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${rdm.loader.server.enabled}")
    private boolean loaderEnabled;

    public RefBookDataServerLoaderRunner(List<ServerLoader> loaders) {
        super(loaders);
    }

    @POST
    @Path("/json/{subject}/{target}")
    @ApiOperation("Загрузить данные")
    @ApiResponse(code = 200, message = "Данные загружены без ошибок")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public void run(@ApiParam("Владелец данных") @PathParam("subject") String subject,
                    @ApiParam("Вид данных") @PathParam("target") String target,
                    InputStream body) {
        super.run(subject, target, body);
    }

    @Override
    protected List<Object> read(InputStream body, LoaderDataInfo<?> info) {

        throw new IllegalArgumentException(String.format("Unsupported format for %s", info.getTarget()));
    }

    @POST
    @Path("/{subject}/{target}")
    @ApiOperation("Загрузить файлы с данными")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Файлы с данными загружены без ошибок"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет файла с данными")
    })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public void runFile(@ApiParam("Владелец данных") @PathParam("subject") String subject,
                        @ApiParam("Вид данных") @PathParam("target") String target,
                        @ApiParam("Содержимое") MultipartBody body) {
        if (!loaderEnabled)
            return;

        ServerLoader loader = find(target);
        RefBookDataRequest request = toRequest(body, loader);
        if (request == null)
            return;

        execute(subject, List.of(request), loader);
    }

    private RefBookDataRequest toRequest(MultipartBody body, LoaderDataInfo<?> info) {

        if (CollectionUtils.isEmpty(body.getAllAttachments()))
            return null;

        RefBookDataRequest request = new RefBookDataRequest();

        for (Attachment attachment : body.getAllAttachments()) {

            String fileName = getFileName(attachment);
            if (!StringUtils.isEmpty(fileName)) {
                FileModel fileModel = read(attachment, fileName, info);
                request.setFileModel(fileModel);
            }

            String name = attachment.getDataHandler().getDataSource().getName();
            String value = attachment.getObject(String.class);

            if ("code".equals(name)) {
                request.setCode(value);
            }
        }

        return request;
    }

    private String getFileName(Attachment attachment) {

        List<String> contentDispositionHeaders = attachment.getHeaderAsList(HttpHeaders.CONTENT_DISPOSITION);
        if (CollectionUtils.isEmpty(contentDispositionHeaders)
                || StringUtils.isEmpty(contentDispositionHeaders.get(0))) {
            return null;
        }

        ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeaders.get(0));
        return contentDisposition.getFilename();
    }
    
    private FileModel read(Attachment attachment, String fileName, LoaderDataInfo<?> info) {
        try {
            return fileStorageService.save(attachment.getDataHandler().getDataSource().getInputStream(), fileName);

        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot read attachment for %s", info.getTarget()), e);
        }
    }
}
