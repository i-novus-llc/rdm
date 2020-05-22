package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
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
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.service.FileStorageService;
import ru.inovus.ms.rdm.api.service.VersionService;

import java.io.IOException;

@RestController
@RequestMapping("/files")
@SuppressWarnings("unused")
public class FilesRestController {

    private final FileStorageService fileStorageService;
    private final VersionService versionService;

    @Value("${rdm.max-file-size-mb:55}")
    private int maxFileSizeMb;

    @Autowired
    private Messages messages;

    @Autowired
    public FilesRestController(FileStorageService fileStorageService, VersionService versionService) {
        this.fileStorageService = fileStorageService;
        this.versionService = versionService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "")
    public FileModel uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        long size = file.getSize();
        if (size / 1024 / 1024 > maxFileSizeMb)
            throw new IllegalArgumentException(messages.getMessage("file.is.too.big", maxFileSizeMb));

        String storageFileName = toStorageFileName(file.getOriginalFilename());
        FileModel save = fileStorageService.save(file.getInputStream(), storageFileName);
        save.setName(file.getOriginalFilename());

        return save;
    }

    private String toStorageFileName(String originalFilename) {

        String extension = FilenameUtils.getExtension(originalFilename);
        if (StringUtils.isEmpty(extension))
            throw new IllegalArgumentException(messages.getMessage("file.extension.invalid"));

        return System.currentTimeMillis() + "." + extension;
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/{versionId}/{type}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer versionId, @PathVariable FileType type) {

        ExportFile versionFile = versionService.getVersionFile(versionId, type);

        return ResponseEntity.ok()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + versionFile.getFileName() + "\"")
                .body(new InputStreamResource(versionFile.getInputStream()));
    }

}
