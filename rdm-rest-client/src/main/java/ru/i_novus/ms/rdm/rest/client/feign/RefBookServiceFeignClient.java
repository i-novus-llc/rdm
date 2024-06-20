package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.RefBookService;

@FeignClient(name = "RefBookServiceFeignClient", url = "${rdm.backend.path}")
public interface RefBookServiceFeignClient extends RefBookService {
}
