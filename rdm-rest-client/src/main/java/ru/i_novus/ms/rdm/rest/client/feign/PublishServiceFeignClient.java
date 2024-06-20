package ru.i_novus.ms.rdm.rest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.PublishService;

@FeignClient(name = "PublishServiceFeignClient", url = "${rdm.backend.path}")
public interface PublishServiceFeignClient extends PublishService {
}
