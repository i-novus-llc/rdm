package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lgalimova
 * @since 21.02.2019
 */

public class RdmSyncService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RefBookService refBookService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private CompareService compareService;

    @Transactional
    public void update(VersionMapping oldVersion, RefBook newVersion) {
        //обновляем версию в таблице версий клиента
        updateVersion(oldVersion.getId(), newVersion.getLastPublishedVersion());

        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setOldVersionId(versionService.getByVersionAndCode(oldVersion.getVersion(), oldVersion.getCode()).getId());
        compareDataCriteria.setNewVersionId(newVersion.getId());
        compareDataCriteria.setCountOnly(true);
        //todo учесть пейджинг
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);
        //todo если изменилась структура и изменения коснулись полей в маппинге, то выбрасываем исключение
        if (diff.getRows().getTotalElements() == 0) {
            //изменений нет
            return;
        }
        compareDataCriteria.setCountOnly(false);
        for (int i = 0; i < diff.getRows().getTotalPages(); i++) {
            compareDataCriteria.setPageNumber(i);
            diff = compareService.compareData(compareDataCriteria);
            for (DiffRowValue row : diff.getRows()) {

            }
        }


    }

    private void updateVersion(Integer id, String newVersion) {
        //todo
    }

    public Map<VersionMapping, RefBook> getRefbooksToUpdate() {
        List<VersionMapping> currentRefbooks = getCurrentVersions();
        Map<VersionMapping, RefBook> refbooksToUpdate = new HashMap<>();
        RefBookCriteria refBookCriteria = new RefBookCriteria();
        for (VersionMapping currentRefbook : currentRefbooks) {
            refBookCriteria.setCode(currentRefbook.getCode());
            //актуальная версия справочника
            Page<RefBook> rdmRefbooks = refBookService.search(refBookCriteria);
            if (rdmRefbooks.isEmpty()) {
                throw new RdmException(String.format("Справочник с кодом %s не найден в системе", currentRefbook.getCode()));
            }
            RefBook rdmRefbook = rdmRefbooks.iterator().next();
            //сравниваем по версии и дате публикации
            if (!currentRefbook.getVersion().equals(rdmRefbook.getLastPublishedVersion()) &&
                    !currentRefbook.getPublicationDate().equals(rdmRefbook.getLastPublishedVersionFromDate())) {
                refbooksToUpdate.put(currentRefbook, rdmRefbook);
            }
        }
        return refbooksToUpdate;
    }

    private List<VersionMapping> getCurrentVersions() {
        return jdbcTemplate.query("select id,code,version,publication_dt,sys_table,unique_rdm_field,deleted_field from rdm_sync.version",
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getDate(4).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)
                ));
    }

}
