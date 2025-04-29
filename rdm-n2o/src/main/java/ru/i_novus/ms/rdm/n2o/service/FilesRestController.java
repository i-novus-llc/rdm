package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.FileUsageTypeEnum;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.FileStorageService;
import ru.i_novus.ms.rdm.n2o.validation.FileValidation;

import java.io.IOException;

import static ru.i_novus.ms.rdm.api.util.FileUtils.getRefBookFileExtension;

@RestController
@RequestMapping("/files")
@SuppressWarnings({"unused", "java:S5122"}) // cors
public class FilesRestController {

    private final FileStorageService fileStorageService;
    private final FileValidation fileValidation;

    private final VersionRestService versionService;

    @Autowired
    public FilesRestController(FileStorageService fileStorageService,
                               FileValidation fileValidation,
                               VersionRestService versionService) {

        this.fileStorageService = fileStorageService;
        this.fileValidation = fileValidation;

        this.versionService = versionService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/refbook")
    public FileModel uploadRefBook(@RequestParam("file") MultipartFile file) throws IOException {

        return uploadFile(file, FileUsageTypeEnum.REF_BOOK);
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/refbook/draft")
    public FileModel uploadRefDraft(@RequestParam("file") MultipartFile file) throws IOException {

        return uploadFile(file, FileUsageTypeEnum.REF_DRAFT);
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/refbook/data")
    public FileModel uploadRefData(@RequestParam("file") MultipartFile file) throws IOException {

        return uploadFile(file, FileUsageTypeEnum.REF_DATA);
    }

    private FileModel uploadFile(MultipartFile file, FileUsageTypeEnum fileUsageType) throws IOException {

        fileValidation.validateSize(file.getSize());

        final String filename = file.getOriginalFilename();
        fileValidation.validateName(filename);
        fileValidation.validateExtensions(filename);

        final String fileExtension = getRefBookFileExtension(filename);
        fileValidation.validateExtensionByUsage(fileExtension, fileUsageType);

        final String storageFileName = toStorageFileName(fileExtension);

        final FileModel save = fileStorageService.save(file.getInputStream(), storageFileName);
        save.setName(filename);

        return save;
    }

    private String toStorageFileName(String extension) {

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
