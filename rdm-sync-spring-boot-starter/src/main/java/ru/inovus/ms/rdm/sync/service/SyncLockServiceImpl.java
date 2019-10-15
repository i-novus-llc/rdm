package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

class SyncLockServiceImpl {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Это чтобы контролировать лочки в пределах одной ноды.
     */
    private static boolean locked;
    private static final Object lock = new Object();

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    boolean tryLock() {
        synchronized (lock) {
            if (locked)
                return false;
            if (getLockTime() != null)
                return false;
            jdbcTemplate.update("UPDATE sync_lock_table SET last_acquired = (SELECT CURRENT_TIMESTAMP AT TIME ZONE 'UTC')");
            locked = true;
            return true;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    void releaseLock() {
        synchronized (lock) {
            if (!locked)
                return;
            if (getLockTime() == null)
                return;
            jdbcTemplate.update("UPDATE sync_lock_table SET last_acquired = NULL");
            locked = false;
        }
    }

    private Timestamp getLockTime() {
        return jdbcTemplate.queryForObject("SELECT last_acquired FROM sync_lock_table", Timestamp.class);
    }

}
