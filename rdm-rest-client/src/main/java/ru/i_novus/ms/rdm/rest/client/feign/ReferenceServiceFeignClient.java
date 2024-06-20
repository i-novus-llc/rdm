package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.ReferenceService;

@FeignClient(name = "ReferenceServiceFeignClient", url = "${rdm.backend.path}")
public interface ReferenceServiceFeignClient extends ReferenceService {
}
