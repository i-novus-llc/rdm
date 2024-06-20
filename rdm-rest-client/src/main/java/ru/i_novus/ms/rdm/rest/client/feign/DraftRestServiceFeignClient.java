package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;

@FeignClient(name = "DraftRestServiceFeignClient", url = "${rdm.backend.path}")
public interface DraftRestServiceFeignClient extends DraftRestService {
}
