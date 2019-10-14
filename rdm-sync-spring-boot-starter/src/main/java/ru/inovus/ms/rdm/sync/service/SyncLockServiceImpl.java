package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class SyncLockServiceImpl {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    boolean tryLock() {
        try {
            jdbcTemplate.execute("LOCK TABLE rdm_sync.sync_lock_table IN SHARE MODE NOWAIT");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
