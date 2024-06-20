package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.CompareService;

@FeignClient(name = "CompareServiceFeignClient", url = "${rdm.backend.path}")
public interface CompareServiceFeignClient extends CompareService {
}
