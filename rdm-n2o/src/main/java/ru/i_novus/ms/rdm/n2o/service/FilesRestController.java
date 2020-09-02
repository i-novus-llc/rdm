package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.exception.FileExtensionException;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.FileStorageService;

import java.io.IOException;

@RestController
@RequestMapping("/files")
@SuppressWarnings("unused")
public class FilesRestController {

    private static final String FILE_IS_TOO_BIG_EXCEPTION_CODE = "file.is.too.big";

    private final FileStorageService fileStorageService;

    private final VersionRestService versionService;

    @Value("${rdm.max-file-size-mb:55}")
    private int maxFileSizeMb;

    @Autowired
    public FilesRestController(FileStorageService fileStorageService,
                               VersionRestService versionService) {

        this.fileStorageService = fileStorageService;

        this.versionService = versionService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "")
    public FileModel uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        long size = file.getSize();
        if (size / 1024 / 1024 > maxFileSizeMb)
            throw new UserException(new Message(FILE_IS_TOO_BIG_EXCEPTION_CODE, maxFileSizeMb));

        String storageFileName = toStorageFileName(file.getOriginalFilename());

        FileModel save = fileStorageService.save(file.getInputStream(), storageFileName);
        save.setName(file.getOriginalFilename());

        return save;
    }

    private String toStorageFileName(String originalFilename) {

        String extension = FilenameUtils.getExtension(originalFilename);
        if (StringUtils.isEmpty(extension))
            throw new FileExtensionException();

        return System.currentTimeMillis() + "." + extension;
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/{versionId}/{type}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer versionId,
                                                 @PathVariable FileType type) {

        ExportFile versionFile = versionService.getVersionFile(versionId, type);

        return ResponseEntity.ok()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + versionFile.getFileName() + "\"")
                .body(new InputStreamResource(versionFile.getInputStream()));
    }
}
