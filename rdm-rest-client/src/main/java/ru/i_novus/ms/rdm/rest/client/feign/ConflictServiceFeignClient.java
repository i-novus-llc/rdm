package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.ConflictService;

@FeignClient(name = "ConflictServiceFeignClient", url = "${rdm.backend.path}")
public interface ConflictServiceFeignClient extends ConflictService {
}
