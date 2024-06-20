package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;

@FeignClient(name = "VersionRestServiceFeignClient", url = "${rdm.backend.path}")
public interface VersionRestServiceFeignClient extends VersionRestService {
}
