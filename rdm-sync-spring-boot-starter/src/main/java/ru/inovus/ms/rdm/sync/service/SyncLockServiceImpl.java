package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

public class SyncLockServiceImpl {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Это чтобы контролировать лочки в пределах одной ноды.
     */
    private static boolean locked;
    private static final Object lock = new Object();

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean tryLock() {
        synchronized (lock) {
            if (locked)
                return false;
            boolean acquired;
            try {
                jdbcTemplate.queryForObject("SELECT last_acquired FROM rdm_sync.sync_lock_table FOR UPDATE NOWAIT", Timestamp.class);
                acquired = true;
            } catch (Exception ex) {
                acquired = false;
            }
            if (acquired) {
                jdbcTemplate.update("UPDATE rdm_sync.sync_lock_table SET last_acquired = (SELECT CURRENT_TIMESTAMP AT TIME ZONE 'UTC')");
                locked = true;
                return true;
            }
            return false;
        }
    }

    /**
     * "LOCK" в Postgresql не имеет "UNLOCK".
     * Лочка релизится автоматом после завершения транзакции.
     * Но тут мы обновляем переменную locked.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public void releaseLock() {
        synchronized (lock) {
            if (!locked)
                return;
            locked = false;
        }
    }

}
