package ru.inovus.ms.rdm.rest.loader;

import io.swagger.annotations.*;
import net.n2oapp.platform.loader.server.BaseLoaderRunner;
import net.n2oapp.platform.loader.server.LoaderDataInfo;
import net.n2oapp.platform.loader.server.ServerLoader;
import net.n2oapp.platform.loader.server.ServerLoaderRestService;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.service.FileStorageService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Запускатель загрузчиков справочников RDM.
 */
@Service
@Api("Загрузчики данных справочников")
public class RefBookDataServerLoaderRunner extends BaseLoaderRunner implements ServerLoaderRestService {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataServerLoaderRunner.class);

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
        List<Object> fileModels = body.getAllAttachments().stream()
                .map(file -> read(file, loader))
                .collect(Collectors.toList());
        execute(subject, fileModels, loader);
    }

    protected FileModel read(Attachment file, LoaderDataInfo<?> info) {
        try {
            String fileName = getFileName(file);
            return fileStorageService.save(file.getDataHandler().getDataSource().getInputStream(), fileName);

        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot read file for %s", info.getTarget()), e);
        }
    }

    private String getFileName(Attachment file) {

        String fileName = file.getDataHandler().getDataSource().getName();
        if (!StringUtils.isEmpty(fileName))
            return fileName;

        List<String> contentDispositionHeaders = file.getHeaderAsList(HttpHeaders.CONTENT_DISPOSITION);
        if (CollectionUtils.isEmpty(contentDispositionHeaders)
                || StringUtils.isEmpty(contentDispositionHeaders.get(0))) {
            logger.error("Content disposition headers are empty");
            throw new NotFoundException();
        }

        ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeaders.get(0));
        fileName = contentDisposition.getFilename();
        if (!StringUtils.isEmpty(fileName))
            return fileName;

        return UUID.randomUUID().toString();
    }
}
