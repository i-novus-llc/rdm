package ru.i_novus.ms.rdm.rest.client.feign.l10n;

import org.springframework.cloud.openfeign.FeignClient;
import ru.i_novus.ms.rdm.api.service.l10n.VersionLocaleService;

@FeignClient(name = "VersionLocaleServiceFeignClient", url = "${rdm.backend.path}")
public interface VersionLocaleServiceFeignClient extends VersionLocaleService {
}
