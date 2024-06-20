package ru.i_novus.ms.rdm.rest.client.feign.l10n;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.l10n.L10nService;

@FeignClient(name = "L10nServiceFeignClient", url = "${rdm.backend.path}")
public interface L10nServiceFeignClient extends L10nService {
}
