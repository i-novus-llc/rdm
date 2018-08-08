package ru.inovus.ms.rdm.util;

import org.apache.cxf.helpers.IOUtils;
import ru.inovus.ms.rdm.model.ExportFile;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Created by znurgaliev on 08.08.2018.
 */
@Provider
@Produces("application/zip")
public class ExportFileProvider implements MessageBodyWriter<ExportFile>, MessageBodyReader<ExportFile> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(ExportFile exportFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(ExportFile exportFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        httpHeaders.put("Content-Disposition", Arrays.asList("attachment; filename=" + exportFile.getFileName()));
        IOUtils.copy(exportFile.getInputStream(), entityStream);
        exportFile.getInputStream().close();
        entityStream.close();
    }


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public ExportFile readFrom(Class<ExportFile> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {

        String name = null;
        List<String> contentDisposition = httpHeaders.get("Content-Disposition");
        String[] parts = new String[0];
        if (contentDisposition != null && !contentDisposition.isEmpty())
            parts = contentDisposition.get(0).split("filename[\\s]*=[\\s]*");
        if (parts.length > 1)
            name = parts[1].split(";")[0];
        return new ExportFile(entityStream, name);
    }
}
