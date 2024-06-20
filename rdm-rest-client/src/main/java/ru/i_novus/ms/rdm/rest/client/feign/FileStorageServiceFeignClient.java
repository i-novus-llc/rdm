package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.FileStorageService;

@FeignClient(name = "FileStorageServiceFeignClient", url = "${rdm.backend.path}")
public interface FileStorageServiceFeignClient extends FileStorageService {
}
